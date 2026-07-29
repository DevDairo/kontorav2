import assert from "node:assert/strict";
import { Writable } from "node:stream";
import test from "node:test";
import { MultiplexedStream } from "../server/docker-api.mjs";

function frame(stream, payload) {
  const body = Buffer.from(payload);
  const header = Buffer.alloc(8);
  header[0] = stream;
  header.writeUInt32BE(body.length, 4);
  return Buffer.concat([header, body]);
}

test("decodifica stdout binario y stderr aunque las tramas lleguen fragmentadas", () => {
  const chunks = [];
  const output = new Writable({
    write(chunk, _encoding, callback) {
      chunks.push(Buffer.from(chunk));
      callback();
    },
  });
  const parser = new MultiplexedStream(output);
  const payload = Buffer.concat([
    frame(1, Buffer.from([0x50, 0x47, 0x44, 0x4d, 0x50])),
    frame(2, "aviso controlado"),
    frame(1, Buffer.from([0x00, 0xff, 0x01])),
  ]);
  parser.write(payload.subarray(0, 3));
  parser.write(payload.subarray(3, 17));
  parser.write(payload.subarray(17));
  parser.finish();

  assert.deepEqual(
    Buffer.concat(chunks),
    Buffer.from([0x50, 0x47, 0x44, 0x4d, 0x50, 0x00, 0xff, 0x01]),
  );
  assert.equal(parser.stderr(), "aviso controlado");
});

test("rechaza una trama Docker incompleta", () => {
  const output = new Writable({
    write(_chunk, _encoding, callback) {
      callback();
    },
  });
  const parser = new MultiplexedStream(output);
  parser.write(frame(1, "dump").subarray(0, 9));
  assert.throws(() => parser.finish(), /trama de ejecución incompleta/);
});

