import { createHmac, timingSafeEqual } from "node:crypto";

const DEFAULT_WINDOW_SECONDS = 600;

function signature(secret, identity, window) {
  return createHmac("sha256", secret)
    .update(`${identity.provider}\n${identity.email}\n${window}`, "utf8")
    .digest("base64url");
}

function constantTimeEquals(left, right) {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);
  return leftBuffer.length === rightBuffer.length
    && timingSafeEqual(leftBuffer, rightBuffer);
}

export function createCsrfGuard(
  secret,
  {
    now = () => Date.now(),
    windowSeconds = DEFAULT_WINDOW_SECONDS,
  } = {},
) {
  if (typeof secret !== "string" || secret.length < 32) {
    throw new Error("El secreto CSRF debe contener al menos 32 caracteres");
  }

  function currentWindow() {
    return Math.floor(now() / 1000 / windowSeconds);
  }

  return {
    issue(identity) {
      const window = currentWindow();
      return `${window}.${signature(secret, identity, window)}`;
    },
    verify(identity, token) {
      const [windowText, receivedSignature, extra] = String(token || "").split(".");
      if (extra !== undefined || !/^\d+$/.test(windowText) || !receivedSignature) {
        return false;
      }
      const window = Number.parseInt(windowText, 10);
      const activeWindow = currentWindow();
      if (window !== activeWindow && window !== activeWindow - 1) {
        return false;
      }
      return constantTimeEquals(
        receivedSignature,
        signature(secret, identity, window),
      );
    },
  };
}

