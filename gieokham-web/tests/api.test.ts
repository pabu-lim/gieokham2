import { test } from "node:test";
import assert from "node:assert/strict";
import { request, resetRequests, ApiError } from "../src/lib/api.ts";
const response = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
test("mutations fetch fresh CSRF and send the session cookie without retrying writes", async () => {
  const saved = globalThis.fetch;
  const calls: { url: string; options?: RequestInit }[] = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url: String(url), options });
    return String(url).endsWith("/csrf")
      ? response({ headerName: "X-CSRF-TOKEN", token: "test-token" })
      : response({ id: 5 });
  };
  try {
    await request("/api/memos", "POST", { content: "메모" });
    assert.equal(calls.length, 2);
    assert.equal(calls[0].url, "/api/auth/csrf");
    assert.equal(
      (calls[1].options?.headers as Record<string, string>)["X-CSRF-TOKEN"],
      "test-token",
    );
    assert.equal(calls[1].options?.credentials, "same-origin");
  } finally {
    globalThis.fetch = saved;
  }
});
test("a response from the previous account is discarded", async () => {
  const saved = globalThis.fetch;
  let finish!: (r: Response) => void;
  globalThis.fetch = () => new Promise((resolve) => (finish = resolve));
  try {
    const pending = request("/api/memos");
    resetRequests();
    finish(response({ content: ["old account"] }));
    await assert.rejects(pending, (e: Error) => e.name === "AbortError");
  } finally {
    globalThis.fetch = saved;
  }
});
test("unauthorized responses notify the application and preserve status", async () => {
  const saved = globalThis.fetch;
  const events = new EventTarget();
  Object.defineProperty(globalThis, "window", {
    value: events,
    configurable: true,
  });
  let expired = false;
  events.addEventListener("gieokham:unauthorized", () => (expired = true));
  globalThis.fetch = async () =>
    response({ message: "로그인이 필요합니다." }, 401);
  try {
    await assert.rejects(
      request("/api/auth/me"),
      (e: ApiError) => e.status === 401,
    );
    assert.equal(expired, true);
  } finally {
    globalThis.fetch = saved;
    Reflect.deleteProperty(globalThis, "window");
  }
});
