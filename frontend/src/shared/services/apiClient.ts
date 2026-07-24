import { API_BASE_URL } from "../utils/env";

type RequestOptions = RequestInit & {
  token?: string;
};

export class ApiClientError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly payload: unknown,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

function resolveUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

async function parseResponse(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (response.status === 204) {
    return null;
  }

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, headers, body, ...init } = options;
  const requestHeaders = new Headers(headers);

  requestHeaders.set("Accept", "application/json");

  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  if (body && !(body instanceof FormData) && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(resolveUrl(path), {
    ...init,
    body,
    headers: requestHeaders,
  });
  const payload = await parseResponse(response);

  if (!response.ok) {
    const message =
      typeof payload === "object" && payload && "mensaje" in payload
        ? String(payload.mensaje)
        : `Error HTTP ${response.status}`;
    throw new ApiClientError(message, response.status, payload);
  }

  return payload as T;
}

async function requestBlob(path: string, options: RequestOptions = {}): Promise<Blob> {
  const { token, headers, body, ...init } = options;
  const requestHeaders = new Headers(headers);

  requestHeaders.set("Accept", "application/octet-stream, application/pdf, image/*");

  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(resolveUrl(path), {
    ...init,
    body,
    headers: requestHeaders,
  });

  if (!response.ok) {
    const payload = await parseResponse(response);
    const message =
      typeof payload === "object" && payload && "mensaje" in payload
        ? String(payload.mensaje)
        : `Error HTTP ${response.status}`;
    throw new ApiClientError(message, response.status, payload);
  }

  return response.blob();
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: BodyInit, options?: RequestOptions) =>
    request<T>(path, { ...options, body, method: "POST" }),
  put: <T>(path: string, body?: BodyInit, options?: RequestOptions) =>
    request<T>(path, { ...options, body, method: "PUT" }),
  getBlob: (path: string, options?: RequestOptions) =>
    requestBlob(path, { ...options, method: "GET" }),
};
