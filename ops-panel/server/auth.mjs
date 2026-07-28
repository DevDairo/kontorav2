import {
  createPublicKey,
  createVerify,
  timingSafeEqual,
} from "node:crypto";

const DEFAULT_JWKS_TTL_MS = 60 * 60 * 1000;

function required(value, name) {
  const normalized = value?.trim();
  if (!normalized) {
    throw new Error(`${name} es obligatorio`);
  }
  return normalized;
}

function normalizeTeamDomain(value) {
  const domain = required(value, "OPS_CF_ACCESS_TEAM_DOMAIN").replace(/\/+$/, "");
  const url = new URL(domain);
  if (url.protocol !== "https:") {
    throw new Error("OPS_CF_ACCESS_TEAM_DOMAIN debe usar HTTPS");
  }
  return url.toString().replace(/\/$/, "");
}

function parseAllowedEmails(value) {
  return new Set(
    required(value, "OPS_ALLOWED_EMAILS")
      .split(",")
      .map((email) => email.trim().toLowerCase())
      .filter(Boolean),
  );
}

export function loadAuthConfig(env = process.env) {
  const mode = (env.OPS_AUTH_MODE || "local-token").trim().toLowerCase();
  const environment = (env.OPS_ENVIRONMENT || "local").trim().toLowerCase();

  if (mode === "local-token") {
    return {
      mode,
      environment,
      localToken: required(env.OPS_LOCAL_TOKEN, "OPS_LOCAL_TOKEN"),
    };
  }

  if (mode === "cloudflare-access") {
    return {
      mode,
      environment,
      teamDomain: normalizeTeamDomain(env.OPS_CF_ACCESS_TEAM_DOMAIN),
      audience: required(env.OPS_CF_ACCESS_AUD, "OPS_CF_ACCESS_AUD"),
      allowedEmails: parseAllowedEmails(env.OPS_ALLOWED_EMAILS),
    };
  }

  throw new Error("OPS_AUTH_MODE solo permite local-token o cloudflare-access");
}

function bearerToken(request) {
  const authorization = request.headers.authorization || "";
  const [scheme, token] = authorization.split(" ");
  if (scheme?.toLowerCase() !== "bearer" || !token) {
    return null;
  }
  return token;
}

function constantTimeEquals(left, right) {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);
  if (leftBuffer.length !== rightBuffer.length) {
    return false;
  }
  return timingSafeEqual(leftBuffer, rightBuffer);
}

function decodeJsonSegment(segment, label) {
  try {
    return JSON.parse(Buffer.from(segment, "base64url").toString("utf8"));
  } catch {
    throw new AuthError(`JWT de Cloudflare con ${label} inválido`);
  }
}

function audienceMatches(claim, expected) {
  if (Array.isArray(claim)) {
    return claim.includes(expected);
  }
  return claim === expected;
}

function validateClaims(payload, config, nowSeconds) {
  if (payload.iss !== config.teamDomain) {
    throw new AuthError("Emisor de Cloudflare Access inválido");
  }
  if (!audienceMatches(payload.aud, config.audience)) {
    throw new AuthError("Audiencia de Cloudflare Access inválida");
  }
  if (typeof payload.exp !== "number" || payload.exp <= nowSeconds) {
    throw new AuthError("Sesión de Cloudflare Access expirada");
  }
  if (typeof payload.nbf === "number" && payload.nbf > nowSeconds + 30) {
    throw new AuthError("Sesión de Cloudflare Access todavía no válida");
  }
  const email = typeof payload.email === "string" ? payload.email.trim().toLowerCase() : "";
  if (!email || !config.allowedEmails.has(email)) {
    throw new AuthError("Identidad no autorizada para el panel");
  }
  return email;
}

export class AuthError extends Error {
  constructor(message, status = 403) {
    super(message);
    this.name = "AuthError";
    this.status = status;
  }
}

export function createAuthenticator(
  config,
  {
    fetchImpl = fetch,
    now = () => Date.now(),
    jwksTtlMs = DEFAULT_JWKS_TTL_MS,
  } = {},
) {
  let jwksCache = null;
  let jwksExpiresAt = 0;

  async function getJwks() {
    if (jwksCache && now() < jwksExpiresAt) {
      return jwksCache;
    }
    const response = await fetchImpl(`${config.teamDomain}/cdn-cgi/access/certs`, {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) {
      throw new AuthError("No fue posible validar la firma de Cloudflare Access", 503);
    }
    const body = await response.json();
    if (!Array.isArray(body.keys)) {
      throw new AuthError("Respuesta de claves de Cloudflare Access inválida", 503);
    }
    jwksCache = body.keys;
    jwksExpiresAt = now() + jwksTtlMs;
    return jwksCache;
  }

  async function authenticateCloudflare(request) {
    const token = request.headers["cf-access-jwt-assertion"];
    if (!token) {
      throw new AuthError("Falta la sesión de Cloudflare Access", 401);
    }

    const parts = token.split(".");
    if (parts.length !== 3) {
      throw new AuthError("JWT de Cloudflare Access inválido");
    }

    const header = decodeJsonSegment(parts[0], "encabezado");
    const payload = decodeJsonSegment(parts[1], "contenido");
    if (header.alg !== "RS256" || typeof header.kid !== "string") {
      throw new AuthError("Algoritmo de Cloudflare Access no permitido");
    }

    const keys = await getJwks();
    const jwk = keys.find((candidate) => candidate.kid === header.kid);
    if (!jwk) {
      jwksExpiresAt = 0;
      const refreshedKeys = await getJwks();
      const refreshedJwk = refreshedKeys.find((candidate) => candidate.kid === header.kid);
      if (!refreshedJwk) {
        throw new AuthError("Clave de firma de Cloudflare Access desconocida");
      }
      return verifyCloudflareSignature(refreshedJwk, parts, payload);
    }
    return verifyCloudflareSignature(jwk, parts, payload);
  }

  function verifyCloudflareSignature(jwk, parts, payload) {
    const verifier = createVerify("RSA-SHA256");
    verifier.update(`${parts[0]}.${parts[1]}`);
    verifier.end();
    const key = createPublicKey({ key: jwk, format: "jwk" });
    if (!verifier.verify(key, Buffer.from(parts[2], "base64url"))) {
      throw new AuthError("Firma de Cloudflare Access inválida");
    }
    const email = validateClaims(payload, config, Math.floor(now() / 1000));
    return {
      email,
      subject: typeof payload.sub === "string" ? payload.sub : null,
      provider: "cloudflare-access",
    };
  }

  return async function authenticate(request) {
    if (config.mode === "local-token") {
      const token = bearerToken(request);
      if (!token || !constantTimeEquals(token, config.localToken)) {
        throw new AuthError("Credencial local inválida", 401);
      }
      return {
        email: "operador-local",
        subject: null,
        provider: "local-token",
      };
    }
    return authenticateCloudflare(request);
  };
}
