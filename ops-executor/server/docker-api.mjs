import { createWriteStream } from "node:fs";
import http from "node:http";
import { Writable } from "node:stream";

const DEFAULT_TIMEOUT_MS = 120_000;
const MAX_ERROR_BYTES = 32_768;

function limitedText(buffers) {
  return Buffer.concat(buffers).subarray(0, MAX_ERROR_BYTES).toString("utf8").trim();
}

function dockerRequest({
  method = "GET",
  pathname,
  body,
  socketPath = "/var/run/docker.sock",
  timeoutMs = DEFAULT_TIMEOUT_MS,
  acceptedStatuses = [200],
}) {
  return new Promise((resolve, reject) => {
    const payload = body === undefined ? null : Buffer.from(JSON.stringify(body));
    const request = http.request({
      socketPath,
      path: pathname,
      method,
      headers: payload
        ? {
            "Content-Type": "application/json",
            "Content-Length": payload.length,
          }
        : {},
    });
    const timer = setTimeout(() => {
      request.destroy(new Error("Docker API superó el tiempo máximo"));
    }, timeoutMs);
    request.on("response", (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("end", () => {
        clearTimeout(timer);
        const responseBody = Buffer.concat(chunks);
        if (!acceptedStatuses.includes(response.statusCode)) {
          reject(new Error(
            `Docker API respondió HTTP ${response.statusCode}: ${limitedText(chunks)}`,
          ));
          return;
        }
        resolve(responseBody);
      });
    });
    request.on("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
    if (payload) {
      request.write(payload);
    }
    request.end();
  });
}

function parseJson(buffer, context) {
  try {
    return JSON.parse(buffer.toString("utf8"));
  } catch {
    throw new Error(`Docker API devolvió JSON inválido al ${context}`);
  }
}

export class MultiplexedStream {
  #buffer = Buffer.alloc(0);
  #stdout;
  #stderr = [];

  constructor(stdout) {
    this.#stdout = stdout;
  }

  write(chunk) {
    this.#buffer = Buffer.concat([this.#buffer, chunk]);
    let writable = true;
    while (this.#buffer.length >= 8) {
      const length = this.#buffer.readUInt32BE(4);
      if (this.#buffer.length < 8 + length) {
        return;
      }
      const stream = this.#buffer[0];
      const payload = this.#buffer.subarray(8, 8 + length);
      this.#buffer = this.#buffer.subarray(8 + length);
      if (stream === 1) {
        writable = this.#stdout.write(payload) && writable;
      } else if (stream === 2 && this.#stderr.reduce((sum, item) => sum + item.length, 0) < MAX_ERROR_BYTES) {
        this.#stderr.push(payload);
      }
    }
    return writable;
  }

  finish() {
    if (this.#buffer.length !== 0) {
      throw new Error("Docker API devolvió una trama de ejecución incompleta");
    }
  }

  stderr() {
    return limitedText(this.#stderr);
  }
}

async function startExecToStream({
  execId,
  stdout,
  socketPath,
  timeoutMs,
}) {
  return new Promise((resolve, reject) => {
    const payload = Buffer.from(JSON.stringify({ Detach: false, Tty: false }));
    const request = http.request({
      socketPath,
      path: `/exec/${encodeURIComponent(execId)}/start`,
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Content-Length": payload.length,
      },
    });
    const parser = new MultiplexedStream(stdout);
    const timer = setTimeout(() => {
      request.destroy(new Error("La ejecución de Docker superó el tiempo máximo"));
    }, timeoutMs);
    request.on("response", (response) => {
      if (response.statusCode !== 200) {
        const chunks = [];
        response.on("data", (chunk) => chunks.push(chunk));
        response.on("end", () => {
          clearTimeout(timer);
          reject(new Error(
            `Docker exec respondió HTTP ${response.statusCode}: ${limitedText(chunks)}`,
          ));
        });
        return;
      }
      const onOutputError = (error) => response.destroy(error);
      stdout.once("error", onOutputError);
      response.on("data", (chunk) => {
        try {
          if (!parser.write(chunk)) {
            response.pause();
            stdout.once("drain", () => response.resume());
          }
        } catch (error) {
          response.destroy(error);
        }
      });
      response.on("end", () => {
        clearTimeout(timer);
        stdout.off("error", onOutputError);
        try {
          parser.finish();
          resolve(parser.stderr());
        } catch (error) {
          reject(error);
        }
      });
      response.on("error", (error) => {
        clearTimeout(timer);
        stdout.off("error", onOutputError);
        reject(error);
      });
    });
    request.on("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
    request.write(payload);
    request.end();
  });
}

export class DockerApi {
  constructor({
    socketPath = "/var/run/docker.sock",
    timeoutMs = DEFAULT_TIMEOUT_MS,
  } = {}) {
    this.socketPath = socketPath;
    this.timeoutMs = timeoutMs;
  }

  async ping() {
    await dockerRequest({
      pathname: "/_ping",
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
      acceptedStatuses: [200],
    });
  }

  async inspectContainer(containerName) {
    const body = await dockerRequest({
      pathname: `/containers/${encodeURIComponent(containerName)}/json`,
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
    });
    return parseJson(body, `inspeccionar ${containerName}`);
  }

  async stopContainer(containerName, seconds = 60) {
    await dockerRequest({
      method: "POST",
      pathname: `/containers/${encodeURIComponent(containerName)}/stop?t=${seconds}`,
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
      acceptedStatuses: [204, 304],
    });
  }

  async startContainer(containerName) {
    await dockerRequest({
      method: "POST",
      pathname: `/containers/${encodeURIComponent(containerName)}/start`,
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
      acceptedStatuses: [204, 304],
    });
  }

  async waitForContainer(containerName, predicate, timeoutMs = this.timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    let lastInspect;
    while (Date.now() < deadline) {
      lastInspect = await this.inspectContainer(containerName);
      if (predicate(lastInspect)) {
        return lastInspect;
      }
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
    throw new Error(
      `El contenedor ${containerName} no alcanzó el estado esperado: ${lastInspect?.State?.Status || "desconocido"}`,
    );
  }

  async execToFile(containerName, command, outputPath) {
    const output = createWriteStream(outputPath, { flags: "wx", mode: 0o600 });
    try {
      return await this.#execToWritable(containerName, command, output);
    } finally {
      await new Promise((resolve, reject) => {
        output.once("error", reject);
        output.end(resolve);
      });
    }
  }

  async execCapture(containerName, command, maximumBytes = 1_048_576) {
    const chunks = [];
    let bytes = 0;
    const output = new Writable({
      write(chunk, _encoding, callback) {
        bytes += chunk.length;
        if (bytes > maximumBytes) {
          callback(new Error("La salida de Docker exec supera el límite permitido"));
          return;
        }
        chunks.push(Buffer.from(chunk));
        callback();
      },
    });
    const result = await this.#execToWritable(containerName, command, output);
    output.end();
    return {
      ...result,
      stdout: Buffer.concat(chunks).toString("utf8"),
    };
  }

  async #execToWritable(containerName, command, output) {
    const created = parseJson(await dockerRequest({
      method: "POST",
      pathname: `/containers/${encodeURIComponent(containerName)}/exec`,
      body: {
        AttachStderr: true,
        AttachStdout: true,
        Cmd: command,
        Tty: false,
      },
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
      acceptedStatuses: [201],
    }), `crear ejecución en ${containerName}`);
    const stderr = await startExecToStream({
      execId: created.Id,
      stdout: output,
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
    });
    const inspected = parseJson(await dockerRequest({
      pathname: `/exec/${encodeURIComponent(created.Id)}/json`,
      socketPath: this.socketPath,
      timeoutMs: this.timeoutMs,
    }), `inspeccionar ejecución en ${containerName}`);
    if (inspected.ExitCode !== 0) {
      throw new Error(
        `La ejecución en ${containerName} terminó con código ${inspected.ExitCode}${stderr ? `: ${stderr}` : ""}`,
      );
    }
    return { exitCode: inspected.ExitCode, stderr };
  }
}
