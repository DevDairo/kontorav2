import { createHmac, randomBytes } from "node:crypto";

const validityYears = Number.parseInt(process.argv[2] ?? "10", 10);

if (!Number.isInteger(validityYears) || validityYears < 1 || validityYears > 20) {
  console.error("Uso: node New-ProductionSecrets.mjs [vigencia-en-anios: 1-20]");
  process.exit(1);
}

function encodeBase64Url(value) {
  return Buffer.from(value).toString("base64url");
}

function randomSecret(size) {
  return randomBytes(size).toString("base64url");
}

function createStorageToken(secret, role) {
  const issuedAt = Math.floor(Date.now() / 1000);
  const expiration = new Date();
  expiration.setUTCFullYear(expiration.getUTCFullYear() + validityYears);

  const header = encodeBase64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = encodeBase64Url(JSON.stringify({
    role,
    iss: "kontora-storage",
    iat: issuedAt,
    exp: Math.floor(expiration.getTime() / 1000),
  }));
  const unsignedToken = `${header}.${payload}`;
  const signature = createHmac("sha256", secret)
    .update(unsignedToken)
    .digest("base64url");

  return `${unsignedToken}.${signature}`;
}

const databasePassword = randomSecret(32);
const applicationJwtSecret = randomSecret(48);
const storageJwtSecret = randomSecret(48);
const storageServiceRoleKey = createStorageToken(storageJwtSecret, "service_role");
const bootstrapManagerPassword = randomSecret(24);

console.log(`DB_PASSWORD=${databasePassword}`);
console.log(`JWT_SECRET=${applicationJwtSecret}`);
console.log(`STORAGE_DATABASE_URL=postgresql://kontora_pos:${databasePassword}@postgres:5432/kontora_pos`);
console.log(`STORAGE_JWT_SECRET=${storageJwtSecret}`);
console.log(`STORAGE_SERVICE_ROLE_KEY=${storageServiceRoleKey}`);
console.log(`BOOTSTRAP_MANAGER_PASSWORD=${bootstrapManagerPassword}`);
console.log("");
console.log("# Valores generados localmente. No los publique ni los reutilice entre entornos.");
