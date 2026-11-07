import type {
  Category,
  Completion,
  Member,
  Memo,
  MemoInput,
  Page,
  SortType,
  Priority,
} from "./model";
export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}
let generation = 0;
export function resetRequests() {
  generation++;
}
async function send<T>(path: string, options: RequestInit = {}): Promise<T> {
  const current = generation;
  let response: Response;
  try {
    response = await fetch(path, {
      ...options,
      credentials: "same-origin",
      cache: "no-store",
    });
  } catch {
    throw new ApiError(
      0,
      "서버에 연결할 수 없습니다. 연결 상태를 확인해 주세요.",
    );
  }
  if (current !== generation)
    throw new DOMException("계정이 변경되었습니다.", "AbortError");
  const text = await response.text();
  if (current !== generation)
    throw new DOMException("계정이 변경되었습니다.", "AbortError");
  let data: any;
  try {
    data = text ? JSON.parse(text) : undefined;
  } catch {
    throw new ApiError(
      response.status,
      "서버 응답을 읽을 수 없습니다. 다시 시도해 주세요.",
    );
  }
  if (!response.ok) {
    if (response.status === 401)
      window.dispatchEvent(new Event("gieokham:unauthorized"));
    const fields = data?.errors ? Object.values(data.errors).join("\n") : "";
    throw new ApiError(
      response.status,
      fields || data?.message || "요청을 처리하지 못했습니다.",
    );
  }
  return data as T;
}
export async function request<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const current = generation;
  const headers: Record<string, string> = {};
  if (method !== "GET") {
    const csrf = await send<{ headerName: string; token: string }>(
      "/api/auth/csrf",
    );
    if (current !== generation)
      throw new DOMException("계정이 변경되었습니다.", "AbortError");
    headers[csrf.headerName] = csrf.token;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  return send<T>(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
export interface Search {
  page?: number;
  size?: number;
  completed?: boolean;
  categoryId?: number;
  priority?: Priority;
  keyword?: string;
  sort?: SortType;
}
export const api = {
  me: () => request<Member>("/api/auth/me"),
  signup: (name: string, nickname: string) =>
    request<Member>("/api/auth/signup", "POST", { name, nickname }),
  logout: () => request<void>("/api/auth/logout", "POST"),
  categories: () => request<Category[]>("/api/categories"),
  createCategory: (name: string, parentId: number | null) =>
    request<Category>("/api/categories", "POST", { name, parentId }),
  renameCategory: (id: number, name: string) =>
    request<Category>(`/api/categories/${id}`, "PATCH", { name }),
  moveCategory: (id: number, parentId: number | null, position: number) =>
    request<Category>(`/api/categories/${id}/move`, "PATCH", {
      parentId,
      position,
    }),
  deleteCategory: (id: number) =>
    request<void>(`/api/categories/${id}`, "DELETE"),
  memos: (search: Search = {}) => {
    const params = new URLSearchParams();
    Object.entries(search).forEach(([k, v]) => {
      if (v !== undefined) params.set(k, String(v));
    });
    return request<Page<Memo>>("/api/memos?" + params);
  },
  memo: (id: number) => request<Memo>(`/api/memos/${id}`),
  createMemo: (body: MemoInput) => request<Memo>("/api/memos", "POST", body),
  updateMemo: (id: number, body: MemoInput) =>
    request<Memo>(`/api/memos/${id}`, "PATCH", body),
  deleteMemo: (id: number) => request<void>(`/api/memos/${id}`, "DELETE"),
  complete: (id: number, completed: boolean) =>
    request<Memo>(`/api/memos/${id}/completion`, "PATCH", { completed }),
  moveMemo: (id: number, nextMemoId: number | null) =>
    request<void>(`/api/memos/${id}/move`, "PATCH", { nextMemoId }),
  today: () => request<Memo[]>("/api/memos/today"),
  completedToday: () => request<number[]>("/api/memos/completions/today"),
  history: (id: number, page = 0) =>
    request<Completion[]>(`/api/memos/${id}/completions?page=${page}&size=20`),
  undoHistory: (id: number, date: string) =>
    request<void>(`/api/memos/${id}/completions/${date}`, "DELETE"),
};
