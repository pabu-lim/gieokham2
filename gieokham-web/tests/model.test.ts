import { test } from "node:test";
import assert from "node:assert/strict";
import {
  blankMemo,
  validateMemo,
  categoryMove,
  categoryTree,
  editable,
  shouldSubmitMemo,
  timestamp,
  createdLabel,
  type Category,
} from "../src/lib/model.ts";
const category = (
  id: number,
  parentId: number | null,
  sortOrder: number,
): Category => ({ id, parentId, sortOrder, name: String(id), memoCount: 0 });
test("IME confirmation, keyCode 229 and Shift+Enter never submit", () => {
  const enter = {
    key: "Enter",
    shiftKey: false,
    isComposing: false,
    keyCode: 13,
  };
  assert.equal(shouldSubmitMemo(enter, false), true);
  assert.equal(shouldSubmitMemo({ ...enter, isComposing: true }, false), false);
  assert.equal(shouldSubmitMemo({ ...enter, keyCode: 229 }, false), false);
  assert.equal(shouldSubmitMemo(enter, true), false);
  assert.equal(shouldSubmitMemo({ ...enter, shiftKey: true }, false), false);
});
test("date-only, equal start/end, independent recurrence and invalid ranges", () => {
  const memo = { ...blankMemo(), content: "운동" };
  assert.equal(validateMemo({ ...memo, repeatType: "DAILY" }), null);
  assert.equal(validateMemo({ ...memo, startDate: "2026-11-06" }), null);
  assert.equal(
    validateMemo({
      ...memo,
      startDate: "2026-11-06",
      endDate: "2026-11-06",
      startTime: "09:00",
      endTime: "09:00",
    }),
    null,
  );
  assert.ok(validateMemo({ ...memo, endDate: "2026-11-06" }));
  assert.ok(
    validateMemo({
      ...memo,
      startDate: "2026-11-06",
      endDate: "2026-11-06",
      startTime: "10:00",
      endTime: "09:00",
    }),
  );
  assert.ok(validateMemo({ ...memo, repeatType: "WEEKLY" }));
  assert.equal(
    validateMemo({
      ...memo,
      repeatType: "YEARLY",
      repeatMonth: 2,
      repeatDayOfMonth: 29,
    }),
    null,
  );
  assert.ok(
    validateMemo({
      ...memo,
      repeatType: "YEARLY",
      repeatMonth: 2,
      repeatDayOfMonth: 30,
    }),
  );
});
test("category nesting and sibling positions exclude the dragged category", () => {
  const cats = [
    category(1, null, 0),
    category(2, null, 1),
    category(3, 1, 0),
    category(4, 3, 0),
  ];
  assert.deepEqual(
    categoryTree(cats).map((c) => c.id),
    [1, 3, 4, 2],
  );
  assert.deepEqual(categoryMove(cats, 1, 2, "after"), {
    parentId: null,
    position: 1,
  });
  assert.deepEqual(categoryMove(cats, 2, 1, "inside"), {
    parentId: 1,
    position: 1,
  });
  assert.throws(() => categoryMove(cats, 1, 4, "inside"));
});
test("response serialization keeps only editable state and canonicalizes weekday ordering", () => {
  const memo = {
    ...blankMemo(),
    id: 1,
    createdAt: "2026-11-06T10:00:00",
    repeatDays: ["FRIDAY", "MONDAY"] as const,
  };
  const result = editable({ ...memo, repeatDays: [...memo.repeatDays] });
  assert.equal("id" in result, false);
  assert.equal("createdAt" in result, false);
  assert.deepEqual(result.repeatDays, ["FRIDAY", "MONDAY"]);
  assert.equal(
    timestamp("2026-11-06T10:00:00"),
    Date.parse("2026-11-06T01:00:00Z"),
  );
  assert.equal(
    createdLabel("2026-11-06T10:00:00", Date.parse("2026-11-06T03:00:00Z")),
    "2시간 전",
  );
});
