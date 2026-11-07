import type { api } from "./api.ts";
import type { Category, Completion, Memo, MemoInput, Page } from "./model.ts";
import { blankMemo, categoryPath, localDate, rank, days } from "./model.ts";
// Explicit, memory-only sandbox. This transport never sends a network request.
export function createDemoApi() {
  let categories: Category[] = ["일상", "취업", "운동", "약속", "공부"].map(
    (name, i) => ({
      id: i + 1,
      name,
      parentId: null,
      sortOrder: i,
      memoCount: 0,
    }),
  );
  let id = 100;
  const date = localDate();
  const now = () =>
    new Date()
      .toLocaleString("sv-SE", { timeZone: "Asia/Seoul" })
      .replace(" ", "T");
  let memos: Memo[] = [
    "샴푸 사기",
    "지원서 제출",
    "운동 20분",
    "친구와 저녁",
    "공부 내용 정리",
  ].map((content, i) => ({
    ...blankMemo(i + 1),
    id: i + 1,
    content,
    priority: i === 1 ? "HIGH" : "MEDIUM",
    startDate: i === 0 ? null : date,
    repeatType: i === 2 ? "DAILY" : "NONE",
    createdAt: now(),
    updatedAt: now(),
    completedAt: null,
  }));
  let history: Completion[] = [];
  const clone = <T>(v: T): T => structuredClone(v);
  const get = (n: number) => {
    const m = memos.find((x) => x.id === n);
    if (!m) throw Error("메모를 찾을 수 없습니다.");
    return m;
  };
  const today = () =>
    memos.filter(
      (m) =>
        !m.completedAt &&
        (m.repeatType === "NONE"
          ? !!m.startDate && m.startDate <= date
          : !history.some(
              (h) => h.memoId === m.id && h.scheduledDate === date,
            ) &&
            (m.repeatType === "DAILY" ||
              (m.repeatType === "WEEKLY" &&
                m.repeatDays.includes(
                  days[(new Date().getDay() + 6) % 7][0],
                )) ||
              (["MONTHLY", "YEARLY"].includes(m.repeatType) &&
                (m.repeatType !== "YEARLY" ||
                  m.repeatMonth === new Date().getMonth() + 1) &&
                Math.min(
                  m.repeatDayOfMonth || 1,
                  new Date(
                    new Date().getFullYear(),
                    new Date().getMonth() + 1,
                    0,
                  ).getDate(),
                ) === new Date().getDate()))),
    );
  memos.sort((a, b) => rank(a.priority) - rank(b.priority) || b.id - a.id);
  const client: typeof api = {
    categories: async () =>
      clone(
        categories.map((c) => ({
          ...c,
          memoCount: memos.filter(
            (m) =>
              !m.completedAt &&
              categoryPath(categories, m.categoryId).some((p) => p.id === c.id),
          ).length,
        })),
      ),
    memos: async (search = {}) => {
      let items = memos.filter(
        (m) =>
          (search.completed === undefined ||
            !!m.completedAt === search.completed) &&
          (search.categoryId === undefined ||
            (search.categoryId === 0
              ? m.categoryId === null
              : categoryPath(categories, m.categoryId).some(
                  (c) => c.id === search.categoryId,
                ))) &&
          (!search.priority || m.priority === search.priority) &&
          (!search.keyword ||
            m.content.toLowerCase().includes(search.keyword.toLowerCase())),
      );
      if (search.sort === "PRIORITY")
        items.sort(
          (a, b) => rank(a.priority) - rank(b.priority) || b.id - a.id,
        );
      if (search.sort === "LATEST") items.sort((a, b) => b.id - a.id);
      const page = search.page || 0,
        size = search.size || 20;
      return clone({
        content: items.slice(page * size, (page + 1) * size),
        page,
        size,
        totalElements: items.length,
        totalPages: Math.ceil(items.length / size),
        hasNext: (page + 1) * size < items.length,
      } satisfies Page<Memo>);
    },
    memo: async (n) => clone(get(n)),
    createMemo: async (body) => {
      const m: Memo = {
        ...clone(body),
        id: ++id,
        createdAt: now(),
        updatedAt: now(),
        completedAt: null,
      };
      let index = memos.findIndex((x) => x.priority === m.priority);
      if (index < 0)
        index = memos.findIndex((x) => rank(x.priority) > rank(m.priority));
      memos.splice(index < 0 ? memos.length : index, 0, m);
      return clone(m);
    },
    updateMemo: async (n, body: MemoInput) => {
      const m = get(n);
      Object.assign(m, clone(body), { updatedAt: now() });
      return clone(m);
    },
    deleteMemo: async (n) => {
      memos = memos.filter((m) => m.id !== n);
      history = history.filter((h) => h.memoId !== n);
    },
    complete: async (n, completed) => {
      const m = get(n);
      if (m.repeatType === "NONE") m.completedAt = completed ? now() : null;
      else {
        history = history.filter(
          (h) => !(h.memoId === n && h.scheduledDate === date),
        );
        if (completed)
          history.push({ memoId: n, scheduledDate: date, completedAt: now() });
      }
      return clone(m);
    },
    today: async () => clone(today()),
    completedToday: async () =>
      history.filter((h) => h.scheduledDate === date).map((h) => h.memoId),
    history: async (n, page = 0) =>
      clone(
        history
          .filter((h) => h.memoId === n)
          .sort((a, b) => b.scheduledDate.localeCompare(a.scheduledDate))
          .slice(page * 20, page * 20 + 20),
      ),
    undoHistory: async (n, date) => {
      history = history.filter(
        (h) => !(h.memoId === n && h.scheduledDate === date),
      );
    },
    moveMemo: async (n, next) => {
      const m = get(n);
      memos = memos.filter((x) => x.id !== n);
      const index =
        next === null ? memos.length : memos.findIndex((x) => x.id === next);
      memos.splice(index < 0 ? memos.length : index, 0, m);
    },
    createCategory: async (name, parentId) => {
      const c = {
        id: ++id,
        name,
        parentId,
        sortOrder: categories.filter((c) => c.parentId === parentId).length,
        memoCount: 0,
      };
      categories.push(c);
      return clone(c);
    },
    renameCategory: async (n, name) => {
      const c = categories.find((c) => c.id === n)!;
      c.name = name;
      return clone(c);
    },
    moveCategory: async (n, parentId, position) => {
      if (categoryPath(categories, parentId).some((c) => c.id === n))
        throw Error("하위 카테고리로 이동할 수 없습니다.");
      const c = categories.find((c) => c.id === n)!;
      c.parentId = parentId;
      const siblings = categories
        .filter((c) => c.id !== n && c.parentId === parentId)
        .sort((a, b) => a.sortOrder - b.sortOrder);
      siblings.splice(position, 0, c);
      siblings.forEach((c, i) => (c.sortOrder = i));
      return clone(c);
    },
    deleteCategory: async (n) => {
      const ids = new Set(
        categories
          .filter((c) => categoryPath(categories, c.id).some((p) => p.id === n))
          .map((c) => c.id),
      );
      const deleted = new Set(
        memos
          .filter((m) => m.categoryId !== null && ids.has(m.categoryId))
          .map((m) => m.id),
      );
      categories = categories.filter((c) => !ids.has(c.id));
      memos = memos.filter((m) => !deleted.has(m.id));
      history = history.filter((h) => !deleted.has(h.memoId));
    },
    me: async () => ({
      registered: true,
      memberId: -1,
      nickname: "체험 사용자",
    }),
    signup: async () => {
      throw Error("체험 공간에서는 가입할 수 없습니다.");
    },
    logout: async () => {},
  };
  return client;
}
