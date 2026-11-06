export type Priority = "HIGHEST" | "HIGH" | "MEDIUM" | "LOW" | "LOWEST";
export type RepeatType = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY";
export type SortType = "CUSTOM" | "PRIORITY" | "LATEST";
export type Day =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";
export interface MemoInput {
  content: string;
  priority: Priority;
  categoryId: number | null;
  startDate: string | null;
  endDate: string | null;
  startTime: string | null;
  endTime: string | null;
  repeatType: RepeatType;
  repeatDays: Day[];
  repeatDayOfMonth: number | null;
  repeatMonth: number | null;
}
export interface Memo extends MemoInput {
  id: number;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  sortOrder: number;
  memoCount: number | null;
}
export interface Completion {
  memoId: number;
  scheduledDate: string;
  completedAt: string;
}
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
export type Member =
  | { registered: true; memberId: number }
  | { registered: false; name: string; nameEditable: boolean };
export const priorities: [Priority, string][] = [
  ["HIGHEST", "최상 · 긴급해요"],
  ["HIGH", "상 · 중요해요"],
  ["MEDIUM", "중 · 보통이에요"],
  ["LOW", "하 · 여유 있어요"],
  ["LOWEST", "최하 · 단순 기록용이에요"],
];
export const days: [Day, string][] = [
  ["MONDAY", "월"],
  ["TUESDAY", "화"],
  ["WEDNESDAY", "수"],
  ["THURSDAY", "목"],
  ["FRIDAY", "금"],
  ["SATURDAY", "토"],
  ["SUNDAY", "일"],
];
export const priorityLabel = (p: Priority) =>
  priorities.find(([id]) => id === p)![1].split(" · ")[0];
export const rank = (p: Priority) => priorities.findIndex(([id]) => id === p);
export const localDate = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
export function blankMemo(categoryId: number | null = null): MemoInput {
  return {
    content: "",
    priority: "MEDIUM",
    categoryId,
    startDate: null,
    endDate: null,
    startTime: null,
    endTime: null,
    repeatType: "NONE",
    repeatDays: [],
    repeatDayOfMonth: null,
    repeatMonth: null,
  };
}
export function editable(m: MemoInput): MemoInput {
  return {
    content: m.content,
    priority: m.priority,
    categoryId: m.categoryId,
    startDate: m.startDate,
    endDate: m.endDate,
    startTime: m.startTime,
    endTime: m.endTime,
    repeatType: m.repeatType,
    repeatDays: [...(m.repeatDays || [])].sort(),
    repeatDayOfMonth: m.repeatDayOfMonth,
    repeatMonth: m.repeatMonth,
  };
}
export function validateMemo(m: MemoInput): string | null {
  if (!m.content.trim()) return "메모 내용을 입력해 주세요.";
  if (m.content.length > 10000)
    return "메모는 10,000자까지 입력할 수 있습니다.";
  if (m.endDate && (!m.startDate || m.endDate < m.startDate))
    return "종료일은 시작일보다 빠를 수 없습니다.";
  if (m.startTime || m.endTime) {
    if (!m.startDate || !m.endDate || !m.startTime || !m.endTime)
      return "시간을 지정하면 시작·종료 일시를 모두 입력해 주세요.";
    if (`${m.endDate}T${m.endTime}` < `${m.startDate}T${m.startTime}`)
      return "종료 일시는 시작 일시보다 빠를 수 없습니다.";
  }
  if (m.repeatType === "WEEKLY" && !m.repeatDays.length)
    return "반복할 요일을 선택해 주세요.";
  if (["MONTHLY", "YEARLY"].includes(m.repeatType)) {
    const max =
      m.repeatType === "YEARLY" && m.repeatMonth
        ? new Date(2024, m.repeatMonth, 0).getDate()
        : 31;
    if (
      !m.repeatDayOfMonth ||
      m.repeatDayOfMonth < 1 ||
      m.repeatDayOfMonth > max
    )
      return "반복할 일자를 확인해 주세요.";
  }
  if (
    m.repeatType === "YEARLY" &&
    (!m.repeatMonth || m.repeatMonth < 1 || m.repeatMonth > 12)
  )
    return "반복할 월을 선택해 주세요.";
  return null;
}
export function categoryPath(
  categories: Category[],
  id: number | null,
): Category[] {
  const byId = new Map(categories.map((c) => [c.id, c]));
  const path: Category[] = [];
  const seen = new Set<number>();
  while (id !== null && !seen.has(id)) {
    seen.add(id);
    const c = byId.get(id);
    if (!c) break;
    path.unshift(c);
    id = c.parentId;
  }
  return path;
}
export function categoryTree(categories: Category[]): Category[] {
  const result: Category[] = [];
  const visited = new Set<number>();
  const visit = (parentId: number | null) =>
    categories
      .filter((c) => c.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
      .forEach((c) => {
        if (visited.has(c.id)) return;
        visited.add(c.id);
        result.push(c);
        visit(c.id);
      });
  visit(null);
  return result;
}
export type CategoryDropEdge = "before" | "after" | "inside";
export function categoryDropEdge(
  y: number,
  top: number,
  height: number,
): CategoryDropEdge {
  const n = (y - top) / height;
  return n < 0.25 ? "before" : n > 0.75 ? "after" : "inside";
}
export function categoryMove(
  categories: Category[],
  source: number,
  target: number,
  edge: CategoryDropEdge,
) {
  const s = categories.find((c) => c.id === source),
    t = categories.find((c) => c.id === target);
  if (!s || !t || s.id === t.id)
    throw Error("이동할 카테고리를 확인해 주세요.");
  if (categoryPath(categories, t.id).some((c) => c.id === s.id))
    throw Error("자신의 하위 카테고리로 이동할 수 없습니다.");
  const parentId = edge === "inside" ? t.id : t.parentId;
  const siblings = categories
    .filter((c) => c.parentId === parentId && c.id !== source)
    .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);
  const position =
    edge === "inside"
      ? siblings.length
      : siblings.findIndex((c) => c.id === target) + (edge === "after" ? 1 : 0);
  return { parentId, position };
}
// Server LocalDateTime values are Korean wall time, not browser-local timestamps.
export const timestamp = (value: string) => Date.parse(value + "+09:00");
export function createdLabel(value: string, now = Date.now()) {
  const diff = Math.max(0, Math.floor((now - timestamp(value)) / 60000));
  return diff < 1
    ? "방금"
    : diff < 60
      ? `${diff}분 전`
      : diff < 1440
        ? `${Math.floor(diff / 60)}시간 전`
        : value.slice(0, 10).replaceAll("-", ".");
}
export const exactTime = (value: string) =>
  value.slice(0, 16).replace("T", " ").replaceAll("-", ".");
export function repeatLabel(m: MemoInput) {
  return {
    NONE: "",
    DAILY: "매일",
    WEEKLY:
      m.repeatDays.map((d) => days.find(([id]) => id === d)?.[1]).join("·") +
      " 반복",
    MONTHLY: `매월 ${m.repeatDayOfMonth}일`,
    YEARLY: `매년 ${m.repeatMonth}월 ${m.repeatDayOfMonth}일`,
  }[m.repeatType];
}
export function scheduleLabel(m: MemoInput) {
  if (!m.startDate) return "";
  return (
    m.startDate.replaceAll("-", ".") +
    (m.startTime ? " " + m.startTime.slice(0, 5) : "") +
    (m.endDate
      ? " ~ " +
        m.endDate.replaceAll("-", ".") +
        (m.endTime ? " " + m.endTime.slice(0, 5) : "")
      : "")
  );
}
export function attention(m: Memo, todayIds: Set<number>, now = Date.now()) {
  const end = m.endDate || m.startDate;
  const time = m.endTime || m.startTime;
  if (m.repeatType === "NONE" && end) {
    const due = timestamp(`${end}T${time || "23:59:59"}`);
    if (due < now) return { label: "기한 지남", tone: "overdue", rank: 0, due };
    if (time && due - now <= 86400000)
      return {
        label: `${Math.ceil((due - now) / 3600000)}시간 이내`,
        tone: "soon",
        rank: 1,
        due,
      };
  }
  if (todayIds.has(m.id))
    return {
      label: m.repeatType === "NONE" ? "오늘" : "오늘의 반복",
      tone: "today",
      rank: 2,
      due: 0,
    };
  if (rank(m.priority) <= 1)
    return {
      label: priorityLabel(m.priority) + " · 중요해요",
      tone: "important",
      rank: 3,
      due: 0,
    };
  return null;
}

export function shouldSubmitMemo(
  event: {
    key: string;
    shiftKey: boolean;
    isComposing: boolean;
    keyCode: number;
  },
  composing: boolean,
) {
  return (
    event.key === "Enter" &&
    !event.shiftKey &&
    !event.isComposing &&
    !composing &&
    event.keyCode !== 229
  );
}
