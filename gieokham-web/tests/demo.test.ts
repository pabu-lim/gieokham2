import { test } from "node:test";
import assert from "node:assert/strict";
import { createDemoApi } from "../src/lib/demo.ts";
import { blankMemo } from "../src/lib/model.ts";
test("demo is isolated; new memo goes to the top of its own priority group", async () => {
  const demo = createDemoApi(),
    other = createDemoApi();
  const high = await demo.createMemo({
    ...blankMemo(),
    content: "긴급",
    priority: "HIGHEST",
  });
  const medium = await demo.createMemo({ ...blankMemo(), content: "중간" });
  const list = (await demo.memos({ size: 100 })).content;
  assert.equal(list[0].id, high.id);
  assert.equal(list.filter((m) => m.priority === "MEDIUM")[0].id, medium.id);
  assert.equal((await other.memos()).totalElements, 5);
  await demo.moveMemo(medium.id, high.id);
  assert.equal((await demo.memos()).content[0].id, medium.id);
  await demo.moveMemo(medium.id, null);
  assert.equal((await demo.memos()).content.at(-1)?.id, medium.id);
});
test("recurring completion leaves the original active and allows history undo", async () => {
  const demo = createDemoApi();
  const m = await demo.createMemo({
    ...blankMemo(),
    content: "매일 운동",
    repeatType: "DAILY",
  });
  await demo.complete(m.id, true);
  assert.equal((await demo.memo(m.id)).completedAt, null);
  assert.ok(!(await demo.today()).some((x) => x.id === m.id));
  const records = await demo.history(m.id);
  assert.equal(records.length, 1);
  await demo.undoHistory(m.id, records[0].scheduledDate);
  assert.ok((await demo.today()).some((x) => x.id === m.id));
});
test("category deletion removes the full subtree and memos but preserves unrelated records", async () => {
  const demo = createDemoApi();
  const root = await demo.createCategory("상위", null),
    child = await demo.createCategory("하위", root.id);
  const m = await demo.createMemo({
    ...blankMemo(child.id),
    content: "하위 기록",
  });
  const keep = await demo.createMemo({ ...blankMemo(), content: "미분류" });
  await demo.deleteCategory(root.id);
  assert.ok(!(await demo.categories()).some((c) => c.id === child.id));
  await assert.rejects(demo.memo(m.id));
  assert.equal((await demo.memo(keep.id)).content, "미분류");
});

test("notes can be saved, reopened and cleared without changing memo content", async () => {
  const demo = createDemoApi();
  const memo = await demo.createMemo({
    ...blankMemo(),
    content: "면접",
    notes: "신분증\nhttps://example.com",
  });
  assert.equal((await demo.memo(memo.id)).notes, memo.notes);
  await demo.updateMemo(memo.id, { ...memo, notes: "준비물 변경" });
  assert.equal((await demo.memo(memo.id)).notes, "준비물 변경");
  await demo.updateMemo(memo.id, { ...memo, notes: "" });
  const saved = await demo.memo(memo.id);
  assert.equal(saved.notes, "");
  assert.equal(saved.content, "면접");
});
