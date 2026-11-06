import "fake-indexeddb/auto";
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  createAccountCache,
  clearEncryptedCache,
  CACHE_TTL_MS,
  expireAccountCache,
} from "../src/lib/encrypted-cache.ts";
test("cache isolates accounts, encrypts payloads, and uses non-extractable keys", async () => {
  await clearEncryptedCache();
  const a = await createAccountCache("a", () => true);
  await a!.write({ secret: "나만의 메모" });
  const restored = await createAccountCache("a", () => true);
  assert.deepEqual(await restored!.read(), { secret: "나만의 메모" });
  const b = await createAccountCache("b", () => true);
  assert.equal(await b!.read(), null);
  const entry: any = await new Promise((resolve, reject) => {
    const r = indexedDB.open("gieokham2-encrypted-cache");
    r.onsuccess = () => {
      const db = r.result;
      const tx = db.transaction("accounts");
      const q = tx.objectStore("accounts").get("a");
      q.onsuccess = () => resolve(q.result);
      tx.oncomplete = () => db.close();
    };
    r.onerror = () => reject(r.error);
  });
  assert.equal(entry.key.extractable, false);
  await assert.rejects(crypto.subtle.exportKey("raw", entry.key));
  assert.ok(entry.cipher instanceof ArrayBuffer);
  assert.ok(!JSON.stringify(entry).includes("나만의 메모"));
});
test("logout cannot be undone by a pending cache write", async () => {
  const cache = await createAccountCache("race", () => true);
  const pending = cache!.write({ content: "stale" });
  await clearEncryptedCache();
  await pending;
  assert.equal(await createAccountCache("race", () => true, false), null);
});
test("expired copies and keys are removed after 24 hours", async () => {
  const cache = await createAccountCache("expired", () => true);
  await cache!.write({ content: "old" });
  const original = Date.now;
  const now = original();
  Date.now = () => now + CACHE_TTL_MS + 1;
  try {
    assert.equal(await expireAccountCache("expired"), true);
    assert.equal(await cache!.read(), null);
    assert.equal(await createAccountCache("expired", () => true, false), null);
  } finally {
    Date.now = original;
  }
});
