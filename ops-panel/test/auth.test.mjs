import assert from "node:assert/strict";
import {
  generateKeyPairSync,
  createSign,
} from "node:crypto";
import test from "node:test";
import { createAuthenticator, loadAuthConfig } from "../server/auth.mjs";

function request(headers = {}) {
  return { headers };
}

function signedJwt(privateKey, kid, payload) {
  const header = Buffer.from(JSON.stringify({ alg: "RS256", kid, typ: "JWT" })).toString("base64url");
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const signer = createSign("RSA-SHA256");
  signer.update(`${header}.${body}`);
  signer.end();
  const signature = signer.sign(privateKey).toString("base64url");
  return `${header}.${body}.${signature}`;
}

test("autenticación local acepta únicamente la credencial configurada", async () => {
  const config = loadAuthConfig({
    OPS_AUTH_MODE: "local-token",
    OPS_LOCAL_TOKEN: "token-seguro",
    OPS_ENVIRONMENT: "local",
  });
  const authenticate = createAuthenticator(config);

  const identity = await authenticate(request({ authorization: "Bearer token-seguro" }));
  assert.equal(identity.email, "operador-local");

  await assert.rejects(
    authenticate(request({ authorization: "Bearer incorrecto" })),
    /Credencial local inválida/,
  );
});

test("Cloudflare Access valida firma, audiencia, emisor y allowlist", async () => {
  const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
  const kid = "test-key";
  const publicJwk = publicKey.export({ format: "jwk" });
  publicJwk.kid = kid;
  publicJwk.alg = "RS256";
  publicJwk.use = "sig";

  const config = loadAuthConfig({
    OPS_AUTH_MODE: "cloudflare-access",
    OPS_ENVIRONMENT: "production",
    OPS_CF_ACCESS_TEAM_DOMAIN: "https://kontora.cloudflareaccess.com",
    OPS_CF_ACCESS_AUD: "audiencia-panel",
    OPS_ALLOWED_EMAILS: "gerencia@example.com",
  });
  const now = 1_800_000_000_000;
  const authenticate = createAuthenticator(config, {
    now: () => now,
    fetchImpl: async () => ({
      ok: true,
      json: async () => ({ keys: [publicJwk] }),
    }),
  });
  const token = signedJwt(privateKey, kid, {
    iss: config.teamDomain,
    aud: [config.audience],
    email: "gerencia@example.com",
    sub: "usuario-1",
    exp: Math.floor(now / 1000) + 300,
  });

  const identity = await authenticate(request({ "cf-access-jwt-assertion": token }));
  assert.equal(identity.email, "gerencia@example.com");
  assert.equal(identity.provider, "cloudflare-access");
});
