import assert from "node:assert/strict";
import test from "node:test";
import { createCsrfGuard } from "../server/csrf.mjs";

const IDENTITY = {
  email: "operador@kontora.test",
  provider: "cloudflare-access",
};

test("token CSRF queda ligado a identidad y ventana temporal", () => {
  let now = Date.parse("2026-07-28T00:00:00Z");
  const guard = createCsrfGuard("s".repeat(43), {
    now: () => now,
    windowSeconds: 600,
  });
  const token = guard.issue(IDENTITY);
  assert.equal(guard.verify(IDENTITY, token), true);
  assert.equal(
    guard.verify({ ...IDENTITY, email: "otro@kontora.test" }, token),
    false,
  );

  now += 10 * 60 * 1000;
  assert.equal(guard.verify(IDENTITY, token), true);
  now += 10 * 60 * 1000;
  assert.equal(guard.verify(IDENTITY, token), false);
});

test("token CSRF alterado o mal formado es rechazado", () => {
  const guard = createCsrfGuard("s".repeat(43), {
    now: () => Date.parse("2026-07-28T00:00:00Z"),
  });
  const token = guard.issue(IDENTITY);
  assert.equal(guard.verify(IDENTITY, `${token}x`), false);
  assert.equal(guard.verify(IDENTITY, "sin-formato"), false);
});

