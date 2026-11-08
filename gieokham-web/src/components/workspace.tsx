import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type DragEvent,
} from "react";
import {
  House,
  Inbox,
  Sun,
  Star,
  Tag,
  CheckCheck,
  Plus,
  Search,
  Settings,
  ArrowUpRight,
  Repeat2,
  Trash2,
  X,
  FlaskConical,
  GripVertical,
  LogOut,
  Cloud,
  LoaderCircle,
  ChevronRight,
  NotebookPen,
  Clock3,
} from "lucide-react";
import {
  SidebarProvider,
  Sidebar,
  SidebarContent,
  SidebarHeader,
  SidebarFooter,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarTrigger,
  useSidebar,
} from "@/components/ui/sidebar";
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { toast } from "sonner";
import { InstallMenuButton } from "@/components/install-app";
import {
  Choice,
  CategoryPriority,
  ScheduleFields,
  clearSchedule,
} from "@/components/memo-fields";
import { useIsMobile } from "@/hooks/use-mobile";
import { useTouchReorder } from "@/hooks/use-touch-reorder";
import { setReorderDragImage } from "@/lib/reorder-preview";
import { api, type Search as MemoSearch } from "@/lib/api";
import {
  clearEncryptedCache,
  createAccountCache,
  expireAccountCache,
} from "@/lib/encrypted-cache";
import {
  blankMemo,
  shouldSubmitMemo,
  categoryPath,
  categoryTree,
  categoryDropEdge,
  categoryMove,
  editable,
  validateMemo,
  priorityLabel,
  createdLabel,
  exactTime,
  repeatLabel,
  scheduleLabel,
  attention,
  rank,
  localDate,
  type Memo,
  type MemoInput,
  type Category,
  type Completion,
  type SortType,
  type CategoryDropEdge,
} from "@/lib/model";

const views = [
  ["home", "홈", House],
  ["all", "전체", Inbox],
  ["today", "오늘", Sun],
  ["important", "중요", Star],
  ["uncategorized", "미분류", Tag],
  ["done", "완료", CheckCheck],
] as const;
const message = (error: unknown) =>
  error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
const aborted = (error: unknown) =>
  error instanceof DOMException && error.name === "AbortError";

function MobileClose() {
  const { isMobile, setOpenMobile } = useSidebar();
  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (isMobile && (e.target as Element).closest("[data-mobile-nav]"))
        setOpenMobile(false);
    };
    document.addEventListener("click", close);
    return () => document.removeEventListener("click", close);
  }, [isMobile, setOpenMobile]);
  return null;
}
async function allMemos(client: typeof api, search: MemoSearch) {
  const result: Memo[] = [];
  for (let page = 0; ; page++) {
    const data = await client.memos({ ...search, page, size: 100 });
    result.push(...data.content);
    if (!data.hasNext) break;
    if (!data.content.length)
      throw Error("목록을 모두 읽지 못했습니다. 다시 조회해 주세요.");
  }
  return result;
}

type Confirmation = {
  title: string;
  body: string;
  label: string;
  danger?: boolean;
  action: () => void | Promise<void>;
};
export function Workspace({
  client,
  demo,
  onExit,
  onDemo,
  memberId,
  nickname,
}: {
  client: typeof api;
  memberId?: number;
  nickname?: string;
  demo: boolean;
  onExit: () => void | Promise<void>;
  onDemo: () => void;
}) {
  const mobile = useIsMobile();
  const [view, setView] = useState("home"),
    [query, setQuery] = useState(""),
    [keyword, setKeyword] = useState(""),
    [sort, setSort] = useState<SortType>("CUSTOM"),
    [page, setPage] = useState(0);
  const [categories, setCategories] = useState<Category[]>([]),
    [memos, setMemos] = useState<Memo[]>([]),
    [homeMemos, setHomeMemos] = useState<Memo[]>([]),
    [todayMemos, setTodayMemos] = useState<Memo[]>([]),
    [doneToday, setDoneToday] = useState<number[]>([]),
    [total, setTotal] = useState(0),
    [hasNext, setHasNext] = useState(false),
    [counts, setCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [refresh, setRefresh] = useState(0);
  const [quick, setQuick] = useState<MemoInput>(() => blankMemo()),
    [expanded, setExpanded] = useState(false),
    [selected, setSelected] = useState<Memo | null>(null),
    [draft, setDraft] = useState<MemoInput | null>(null),
    [confirmation, setConfirmation] = useState<Confirmation | null>(null),
    [settings, setSettings] = useState(false);
  const [categoryEditor, setCategoryEditor] = useState<{
      id: number | null;
      name: string;
      parentId: number | null;
    } | null>(null),
    [collapsed, setCollapsed] = useState<Set<number>>(new Set());
  const [drag, setDrag] = useState<{
      kind: "task" | "category";
      id: string;
    } | null>(null),
    [drop, setDrop] = useState<{
      kind: "task" | "category";
      id: string;
      edge: CategoryDropEdge;
    } | null>(null);
  const [historyExpanded, setHistoryExpanded] = useState(false),
    [historyLoading, setHistoryLoading] = useState(false),
    [history, setHistory] = useState<Completion[]>([]),
    [historyPage, setHistoryPage] = useState(0),
    [historyMore, setHistoryMore] = useState(false),
    [historyError, setHistoryError] = useState("");
  const [now, setNow] = useState(Date.now()),
    [showAllAttention, setShowAllAttention] = useState(false);
  const cache = useRef<Awaited<ReturnType<typeof createAccountCache>>>(null),
    serverReady = useRef(false);
  const [cached, setCached] = useState(false);
  const blocked = busy || loading || !!error || cached;
  useEffect(() => {
    if (!memberId || demo || import.meta.env.DEV) return;
    let active = true;
    const timer = setInterval(() => {
      void expireAccountCache(String(memberId)).catch(() => {});
    }, 60000);
    void createAccountCache(String(memberId), () => active)
      .then(async (store) => {
        if (!store || !active) return;
        cache.current = store;
        const snapshot = (await store.read()) as {
          version?: number;
          categories?: Category[];
          memos?: Memo[];
          today?: Memo[];
          done?: number[];
          counts?: Record<string, number>;
        } | null;
        if (
          !active ||
          serverReady.current ||
          snapshot?.version !== 1 ||
          !Array.isArray(snapshot.categories) ||
          !Array.isArray(snapshot.memos) ||
          !Array.isArray(snapshot.today) ||
          !Array.isArray(snapshot.done) ||
          !snapshot.counts
        )
          return;
        setCategories(snapshot.categories);
        setHomeMemos(snapshot.memos);
        setTodayMemos(snapshot.today);
        setDoneToday(snapshot.done);
        setCounts(snapshot.counts);
        setCached(true);
      })
      .catch(() => {});
    return () => {
      active = false;
      clearInterval(timer);
      cache.current = null;
    };
  }, [memberId, demo]);
  const quickRef = useRef<HTMLTextAreaElement>(null),
    composing = useRef(false),
    busyRef = useRef(false),
    revision = useRef(0),
    live = useRef(true),
    historyRevision = useRef(0);
  const swipe = useRef<{ x: number; y: number } | null>(null),
    dragRef = useRef<typeof drag>(null);
  const todayIds = new Set(todayMemos.map((m) => m.id));
  const title =
    views.find((v) => v[0] === view)?.[1] ||
    categories.find((c) => view === `cat:${c.id}`)?.name ||
    "카테고리";
  useEffect(() => {
    const timeout = setTimeout(() => {
      setKeyword(query);
      setPage(0);
    }, 250);
    return () => clearTimeout(timeout);
  }, [query]);
  useEffect(() => {
    setQuick((q) => ({
      ...q,
      categoryId: view.startsWith("cat:") ? Number(view.slice(4)) : null,
    }));
  }, [view]);
  useEffect(() => {
    const el = quickRef.current;
    if (el) {
      el.style.height = "auto";
      el.style.height = Math.min(el.scrollHeight, 200) + "px";
    }
  }, [quick.content]);
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
      revision.current++;
      historyRevision.current++;
    };
  }, []);
  useEffect(() => {
    let day = localDate();
    const timer = setInterval(() => {
      setNow(Date.now());
      if (localDate() !== day) {
        day = localDate();
        setRefresh((n) => n + 1);
      }
    }, 30000);
    return () => clearInterval(timer);
  }, []);
  useEffect(() => {
    if (!mobile || !selected) return;
    const body = document.body;
    const previous = body.style.overflow;
    body.style.overflow = "hidden";
    return () => {
      body.style.overflow = previous;
    };
  }, [mobile, selected?.id]);
  async function reload() {
    const version = ++revision.current;
    setLoading(true);
    setError("");
    try {
      const normal = !["home", "today", "important"].includes(view);
      const search: MemoSearch = {
        page,
        size: 30,
        sort,
        completed: view === "done",
        keyword: keyword || undefined,
        categoryId:
          view === "uncategorized"
            ? 0
            : view.startsWith("cat:")
              ? Number(view.slice(4))
              : undefined,
      };
      const [
        cats,
        today,
        completed,
        activeCount,
        doneCount,
        uncategorized,
        data,
      ] = await Promise.all([
        client.categories(),
        client.today(),
        client.completedToday(),
        client.memos({ completed: false, size: 1 }),
        client.memos({ completed: true, size: 1 }),
        client.memos({ completed: false, categoryId: 0, size: 1 }),
        normal
          ? client.memos(search)
          : allMemos(client, {
              completed: false,
              sort: view === "home" ? "LATEST" : sort,
            }),
      ]);
      if (version !== revision.current || !live.current) return;
      serverReady.current = true;
      setCached(false);
      setCategories(cats);
      setTodayMemos(today);
      setDoneToday(completed);
      setCounts({
        all: activeCount.totalElements,
        done: doneCount.totalElements,
        uncategorized: uncategorized.totalElements,
        today: today.length,
      });
      if (Array.isArray(data)) {
        setHomeMemos(data);
        if (view === "home" && memberId && !demo && import.meta.env.PROD) {
          const snapshot = {
            version: 1,
            categories: cats,
            memos: data,
            today,
            done: completed,
            counts: {
              all: activeCount.totalElements,
              done: doneCount.totalElements,
              uncategorized: uncategorized.totalElements,
              today: today.length,
            },
          };
          void (async () => {
            const store =
              cache.current ??
              (await createAccountCache(String(memberId), () => live.current));
            cache.current = store;
            await store?.write(snapshot);
          })().catch(() => {});
        }
        const ids = new Set(today.map((m) => m.id));
        const filtered = data.filter(
          (m) =>
            (view !== "today" || ids.has(m.id)) &&
            (view !== "important" || rank(m.priority) <= 1) &&
            (!keyword ||
              m.content.toLowerCase().includes(keyword.toLowerCase())),
        );
        if (page > 0 && page * 30 >= filtered.length) {
          setPage(Math.max(0, Math.ceil(filtered.length / 30) - 1));
        }
        setMemos(filtered.slice(page * 30, (page + 1) * 30));
        setTotal(filtered.length);
        setHasNext((page + 1) * 30 < filtered.length);
      } else {
        setMemos(data.content);
        setTotal(data.totalElements);
        setHasNext(data.hasNext);
        if (page > 0 && !data.content.length) setPage(page - 1);
      }
    } catch (e) {
      if (version !== revision.current || !live.current || aborted(e)) return;
      setError(message(e));
      throw e;
    } finally {
      if (version === revision.current && live.current) setLoading(false);
    }
  }
  useEffect(() => {
    void reload().catch(() => {});
  }, [view, keyword, sort, page, refresh]);
  async function perform(action: () => Promise<void>, success?: string) {
    if (busyRef.current || loading || !!error || cached) return false;
    busyRef.current = true;
    setBusy(true);
    let saved = false;
    try {
      if (!demo) {
        cache.current = null;
        await clearEncryptedCache().catch(() => {});
      }
      await action();
      saved = true;
      if (!live.current) return true;
      await reload();
      if (success) toast.success(success);
      return true;
    } catch (e) {
      if (!aborted(e) && live.current)
        toast.error(
          saved
            ? "저장은 완료됐지만 목록을 다시 불러오지 못했습니다. 새로고침해 주세요."
            : message(e),
        );
      return false;
    } finally {
      busyRef.current = false;
      if (live.current) setBusy(false);
    }
  }
  const dirty = () =>
    !!selected &&
    !!draft &&
    JSON.stringify(editable(selected)) !== JSON.stringify(editable(draft));
  function guard(action: () => void | Promise<void>) {
    if (dirty())
      setConfirmation({
        title: "변경 내용을 저장하지 않고 나갈까요?",
        body: "나가기를 누르면 저장하지 않은 수정 내용이 사라집니다.\n계속 수정하려면 취소를 누르세요.",
        label: "나가기",
        danger: true,
        action,
      });
    else void action();
  }
  function closeDetail() {
    if (busyRef.current) return;
    guard(() => {
      setSelected(null);
      setDraft(null);
    });
  }
  useEffect(() => {
    if (!dirty()) return;
    const leave = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", leave);
    return () => window.removeEventListener("beforeunload", leave);
  }, [selected, draft]);
  function navigate(next: string) {
    guard(() => {
      setSelected(null);
      setDraft(null);
      setView(next);
      setQuery("");
      setKeyword("");
      setPage(0);
    });
  }
  function open(m: Memo) {
    guard(() => {
      setSelected(m);
      setDraft(editable(m));
      setHistoryExpanded(false);
      setHistoryPage(0);
      setHistory([]);
    });
  }
  async function loadHistory(id: number, p: number) {
    const version = ++historyRevision.current;
    setHistoryError("");
    setHistoryLoading(true);
    try {
      const entries = await client.history(id, p);
      if (version !== historyRevision.current || !live.current) return;
      setHistory(entries);
      setHistoryMore(entries.length === 20);
    } catch (e) {
      if (version === historyRevision.current && !aborted(e))
        setHistoryError(message(e));
    } finally {
      if (version === historyRevision.current && live.current) setHistoryLoading(false);
    }
  }
  useEffect(() => {
    if (selected && selected.repeatType !== "NONE" && historyExpanded) {
      void loadHistory(selected.id, historyPage);
    } else {
      historyRevision.current++;
      setHistory([]);
    }
  }, [selected?.id, selected?.repeatType, historyPage, historyExpanded]);
  async function addMemo() {
    const problem = validateMemo(quick);
    if (problem) {
      toast.error(problem);
      return;
    }
    const body = editable(quick);
    await perform(async () => {
      await client.createMemo(body);
      if (!live.current) return;
      setQuick(blankMemo(body.categoryId));
      setExpanded(false);
      setView(body.categoryId ? `cat:${body.categoryId}` : "uncategorized");
      setPage(0);
      setQuery("");
      setKeyword("");
      requestAnimationFrame(() => quickRef.current?.focus());
    }, "기억을 남겼습니다.");
  }
  async function saveMemo() {
    if (!selected || !draft) return;
    const problem = validateMemo(draft);
    if (problem) {
      toast.error(problem);
      return;
    }
    const id = selected.id,
      body = editable(draft);
    await perform(async () => {
      await client.updateMemo(id, body);
      setSelected(null);
      setDraft(null);
    }, "변경 내용을 저장했습니다.");
  }
  async function complete(m: Memo) {
    const completed =
      m.repeatType === "NONE" ? !!m.completedAt : doneToday.includes(m.id);
    await perform(
      async () => {
        const next = await client.complete(m.id, !completed);
        if (selected?.id === m.id) {
          setSelected(next);
          if (next.repeatType !== "NONE" && historyExpanded) await loadHistory(m.id, historyPage);
        }
      },
      completed ? "완료를 취소했습니다." : "완료했습니다.",
    );
  }
  function deleteMemo(m: Memo) {
    setConfirmation({
      title: "메모를 삭제할까요?",
      body: "메모와 반복 완료 기록이 함께 삭제됩니다. 삭제 후에는 되돌릴 수 없습니다.",
      label: "삭제",
      danger: true,
      action: async () => {
        await perform(async () => {
          await client.deleteMemo(m.id);
          setSelected(null);
          setDraft(null);
        }, "삭제했습니다.");
      },
    });
  }
  async function saveCategory() {
    if (!categoryEditor || !categoryEditor.name.trim()) {
      toast.error("카테고리 이름을 입력해 주세요.");
      return;
    }
    const edit = { ...categoryEditor };
    await perform(async () => {
      if (edit.id === null)
        await client.createCategory(edit.name, edit.parentId);
      else {
        const old = categories.find((c) => c.id === edit.id)!;
        if (old.name !== edit.name)
          await client.renameCategory(edit.id, edit.name);
        if (old.parentId !== edit.parentId) {
          try {
            await client.moveCategory(
              edit.id,
              edit.parentId,
              categories.filter(
                (c) => c.parentId === edit.parentId && c.id !== edit.id,
              ).length,
            );
          } catch (e) {
            setRefresh((n) => n + 1);
            throw new Error(
              "이름 변경은 저장됐지만 상위 카테고리 이동에 실패했습니다. " +
                message(e),
            );
          }
        }
      }
      setCategoryEditor(null);
    }, "카테고리를 저장했습니다.");
  }
  function deleteCategory(c: Category) {
    setConfirmation({
      title: `‘${c.name}’을 삭제할까요?`,
      body: "하위 카테고리와 그 안의 모든 메모·완료 기록도 삭제됩니다. 되돌릴 수 없습니다.",
      label: "삭제",
      danger: true,
      action: async () => {
        await perform(async () => {
          await client.deleteCategory(c.id);
          setCategoryEditor(null);
          setSelected(null);
          setDraft(null);
          if (
            view.startsWith("cat:") &&
            categoryPath(categories, Number(view.slice(4))).some(
              (p) => p.id === c.id,
            )
          )
            setView("all");
        }, "카테고리를 삭제했습니다.");
      },
    });
  }
  function endDrag() {
    dragRef.current = null;
    setDrag(null);
    setDrop(null);
  }
  async function move(
    kind: "category" | "task",
    source: string,
    target: string,
    edge: CategoryDropEdge,
  ) {
    if (busyRef.current || loading || source === target) return;
    if (kind === "category") {
      try {
        const result = categoryMove(
          categories,
          Number(source),
          Number(target),
          edge,
        );
        await perform(async () => {
          await client.moveCategory(
            Number(source),
            result.parentId,
            result.position,
          );
        }, "카테고리 위치를 저장했습니다.");
      } catch (e) {
        toast.error(message(e));
      }
      return;
    }
    if (sort !== "CUSTOM" || view === "home" || edge === "inside") return;
    const without = memos.filter((m) => m.id !== Number(source));
    const index =
      without.findIndex((m) => m.id === Number(target)) +
      (edge === "after" ? 1 : 0);
    if (index < 0) return;
    if (index === without.length && hasNext) {
      toast.info("다음 페이지가 있습니다. 기준 메모의 위쪽에 놓아 주세요.");
      return;
    }
    const nextMemoId = without[index]?.id ?? null;
    const moved = memos.find((m) => m.id === Number(source));
    if (!moved) return;
    const optimistic = [...without];
    optimistic.splice(index, 0, moved);
    setMemos(optimistic);
    const ok = await perform(async () => {
      await client.moveMemo(moved.id, nextMemoId);
    }, "메모 순서를 저장했습니다.");
    if (!ok) {
      setRefresh((n) => n + 1);
    }
  }
  useTouchReorder({
    context: JSON.stringify([sort, view, keyword, busy, loading]),
    onStart: (kind, id) => {
      setDrag({ kind, id });
    },
    onTarget: (kind, id, edge) => setDrop(id ? { kind, id, edge } : null),
    onDrop: (kind, source, target, edge) =>
      void move(kind, source, target, edge),
    onEnd: endDrag,
  });
  function dragStart(
    e: DragEvent<HTMLButtonElement>,
    kind: "task" | "category",
    id: number,
  ) {
    setReorderDragImage(e);
    dragRef.current = { kind, id: String(id) };
    setDrag(dragRef.current);
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(id));
  }
  function dragOver(
    e: DragEvent<HTMLElement>,
    kind: "task" | "category",
    id: number,
  ) {
    if (busy || loading || dragRef.current?.kind !== kind) return;
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    const edge =
      kind === "category"
        ? categoryDropEdge(e.clientY, rect.top, rect.height)
        : e.clientY < rect.top + rect.height / 2
          ? "before"
          : "after";
    setDrop({ kind, id: String(id), edge });
  }
  function dropAt(
    e: DragEvent<HTMLElement>,
    kind: "task" | "category",
    id: number,
  ) {
    e.preventDefault();
    const source = dragRef.current;
    if (source?.kind === kind) {
      const rect = e.currentTarget.getBoundingClientRect();
      void move(
        kind,
        source.id,
        String(id),
        kind === "category"
          ? categoryDropEdge(e.clientY, rect.top, rect.height)
          : e.clientY < rect.top + rect.height / 2
            ? "before"
            : "after",
      );
    }
    endDrag();
  }
  function row(m: Memo, compact = false, showStatus = false) {
    const done =
      m.repeatType === "NONE" ? !!m.completedAt : doneToday.includes(m.id);
    const label = priorityLabel(m.priority);
    const state = attention(m, todayIds, now);
    const draggable = !compact && sort === "CUSTOM" && !blocked;
    return (
      <article
        key={m.id}
        className={
          (compact ? "home-memory" : "task-row") +
          (selected?.id === m.id ? " selected" : "") +
          (done ? " is-done" : "") +
          (drag?.kind === "task" && drag.id === String(m.id)
            ? " task-dragging"
            : "") +
          (drop?.kind === "task" && drop.id === String(m.id)
            ? " task-drop-" + drop.edge
            : "")
        }
        data-touch-reorder={draggable ? "task" : undefined}
        data-reorder-id={m.id}
        onDragOver={(e) => dragOver(e, "task", m.id)}
        onDrop={(e) => dropAt(e, "task", m.id)}
      >
        {!compact && sort === "CUSTOM" && (
          <button
            className="task-grip"
            aria-label={m.content + " 순서 이동"}
            draggable={draggable}
            disabled={blocked}
            onDragStart={(e) => dragStart(e, "task", m.id)}
            onDragEnd={endDrag}
            onKeyDown={(e) => {
              if (e.altKey && (e.key === "ArrowUp" || e.key === "ArrowDown")) {
                e.preventDefault();
                const pos = memos.findIndex((x) => x.id === m.id);
                const target = memos[pos + (e.key === "ArrowUp" ? -1 : 1)];
                if (target)
                  void move(
                    "task",
                    String(m.id),
                    String(target.id),
                    e.key === "ArrowUp" ? "before" : "after",
                  );
              }
            }}
          >
            <GripVertical size={15} />
          </button>
        )}
        <Checkbox
          aria-label={m.content + (done ? " 완료 취소" : " 완료")}
          checked={done}
          disabled={blocked}
          onCheckedChange={() => void complete(m)}
        />
        <button
          className={compact ? "home-memory-content" : "task-body"}
          onClick={() => open(m)}
        >
          {showStatus && state && (
            <span className={"home-status " + state.tone}>{state.label}</span>
          )}
          <span className={compact ? "home-memory-title" : "task-title"}>
            {m.content}
          </span>
          {compact ? (
            <span className="home-memory-meta">
              {[
                categories.find((c) => c.id === m.categoryId)?.name || "미분류",
                m.startDate && scheduleLabel(m),
                m.notes?.trim() && "추가 메모",
              ]
                .filter(Boolean)
                .join(" · ")}
            </span>
          ) : (
            <span className="task-meta">
              {!view.startsWith("cat:") && view !== "uncategorized" && (
                <span>
                  {categories.find((c) => c.id === m.categoryId)?.name ||
                    "미분류"}
                </span>
              )}
              {m.startDate && (
                <span className={state?.tone === "overdue" ? "overdue" : ""}>
                  {scheduleLabel(m)}
                </span>
              )}
              {m.repeatType !== "NONE" && (
                <span>
                  <Repeat2 size={12} />
                  {repeatLabel(m)}
                  {done ? " · 오늘 완료" : ""}
                </span>
              )}
            </span>
          )}
          {!compact && m.notes?.trim() && (
            <span className="task-notes-preview">
              {m.notes.trim().split(/\r?\n/)[0]}
            </span>
          )}
        </button>
        {compact ? (
          <span className={"priority p" + label}>{label}</span>
        ) : (
          <div className="task-trailing">
            <span className={"priority p" + label}>{label}</span>
            <time
              className="task-time"
              dateTime={m.createdAt + "+09:00"}
              title={exactTime(m.createdAt)}
            >
              {createdLabel(m.createdAt, now)}
            </time>
          </div>
        )}
      </article>
    );
  }
  const attentionMemos = homeMemos
    .filter((m) => !doneToday.includes(m.id))
    .map((m) => ({ memo: m, state: attention(m, todayIds, now) }))
    .filter((x) => x.state !== null)
    .sort(
      (a, b) =>
        a.state!.rank - b.state!.rank ||
        a.state!.due - b.state!.due ||
        rank(a.memo.priority) - rank(b.memo.priority),
    );
  return (
    <SidebarProvider
      style={
        {
          "--sidebar-width": "clamp(250px, calc(15.625vw + 85px), 310px)",
        } as CSSProperties
      }
    >
      <MobileClose />
      <Sidebar className="nav-side">
        <SidebarHeader>
          <button
            className="brand"
            aria-label="기억함 · 홈으로 새로고침"
            onClick={() => guard(() => location.assign("/"))}
          >
            <span>
              <img src="/icons/memo-cutout-192.png" alt="" />
            </span>
            <img
              className="brand-wordmark"
              src="/icons/memo-wordmark.png"
              alt="기억함"
            />
          </button>
          <p className="workspace-label">나의 작은 기억 보관함</p>
        </SidebarHeader>
        <SidebarContent>
          <SidebarMenu>
            {views.map(([id, name, Icon]) => (
              <SidebarMenuItem key={id}>
                <SidebarMenuButton
                  data-mobile-nav
                  isActive={view === id}
                  onClick={() => navigate(id)}
                  className="nav-button"
                >
                  <Icon />
                  <span>{name}</span>
                  {counts[id] !== undefined && <b>{counts[id]}</b>}
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}
          </SidebarMenu>
          <div className="category-heading">
            카테고리
            <button
              aria-label="카테고리 추가"
              disabled={busy}
              onClick={() =>
                setCategoryEditor({ id: null, name: "", parentId: null })
              }
            >
              <Plus size={17} />
            </button>
          </div>
          <SidebarMenu>
            {categoryTree(categories)
              .filter(
                (c) =>
                  !categoryPath(categories, c.id)
                    .slice(0, -1)
                    .some((p) => collapsed.has(p.id)),
              )
              .map((c, i) => (
                <SidebarMenuItem
                  key={c.id}
                  style={{
                    marginLeft:
                      Math.min(categoryPath(categories, c.id).length - 1, 4) *
                      14,
                  }}
                  className={
                    "category-row" +
                    (drag?.kind === "category" && drag.id === String(c.id)
                      ? " category-dragging"
                      : "") +
                    (drop?.kind === "category" && drop.id === String(c.id)
                      ? " drop-" + drop.edge
                      : "")
                  }
                  data-touch-reorder={!blocked ? "category" : undefined}
                  data-reorder-id={c.id}
                  onDragOver={(e) => dragOver(e, "category", c.id)}
                  onDrop={(e) => dropAt(e, "category", c.id)}
                >
                  <button
                    className="category-grip"
                    draggable={!blocked}
                    aria-label={c.name + " 순서 이동"}
                    onDragStart={(e) => dragStart(e, "category", c.id)}
                    onDragEnd={endDrag}
                    onKeyDown={(e) => {
                      if (
                        e.altKey &&
                        (e.key === "ArrowUp" || e.key === "ArrowDown")
                      ) {
                        e.preventDefault();
                        const siblings = categories
                          .filter((s) => s.parentId === c.parentId)
                          .sort((a, b) => a.sortOrder - b.sortOrder);
                        const target =
                          siblings[
                            siblings.findIndex((s) => s.id === c.id) +
                              (e.key === "ArrowUp" ? -1 : 1)
                          ];
                        if (target)
                          void move(
                            "category",
                            String(c.id),
                            String(target.id),
                            e.key === "ArrowUp" ? "before" : "after",
                          );
                      }
                    }}
                  >
                    <GripVertical size={15} />
                  </button>
                  {categories.some((child) => child.parentId === c.id) ? (
                    <button
                      className="category-toggle"
                      aria-label={
                        c.name + (collapsed.has(c.id) ? " 펼치기" : " 접기")
                      }
                      aria-expanded={!collapsed.has(c.id)}
                      onClick={() =>
                        setCollapsed((prev) => {
                          const next = new Set(prev);
                          next.has(c.id) ? next.delete(c.id) : next.add(c.id);
                          return next;
                        })
                      }
                    >
                      <ChevronRight
                        size={15}
                        className={collapsed.has(c.id) ? "" : "expanded"}
                      />
                    </button>
                  ) : (
                    <span
                      className={"category-color category-leaf c" + (i % 6)}
                    >
                      #
                    </span>
                  )}
                  <SidebarMenuButton
                    data-mobile-nav
                    title={c.name}
                    className="nav-button"
                    isActive={view === `cat:${c.id}`}
                    onClick={() => navigate(`cat:${c.id}`)}
                  >
                    <span className="category-name">{c.name}</span>
                    <b className="category-count">{c.memoCount ?? 0}</b>
                  </SidebarMenuButton>
                  <button
                    className="rename"
                    aria-label={c.name + " 카테고리 관리"}
                    onClick={() =>
                      setCategoryEditor({
                        id: c.id,
                        name: c.name,
                        parentId: c.parentId,
                      })
                    }
                  >
                    ···
                  </button>
                  {drop?.kind === "category" &&
                    drop.id === String(c.id) &&
                    drop.edge === "inside" && (
                      <span className="category-nest-hint">하위로 이동</span>
                    )}
                </SidebarMenuItem>
              ))}
          </SidebarMenu>
        </SidebarContent>
        <SidebarFooter>
          <button
            data-mobile-nav
            className="demo-button"
            onClick={() =>
              guard(() => {
                if (demo) void onExit();
                else onDemo();
              })
            }
          >
            <FlaskConical size={17} />
            {demo ? "내 기록으로 돌아가기" : "예시로 체험하기"}
            <ArrowUpRight size={15} />
          </button>
          <InstallMenuButton />
          <button
            data-mobile-nav
            className="settings-button"
            onClick={() => setSettings(true)}
          >
            <Settings size={17} />
            설정
          </button>
          <span className="local-label">
            <Cloud size={13} />
            {demo ? "별도의 체험 공간" : busy ? "저장 중" : "내 계정에 저장"}
          </span>
        </SidebarFooter>
      </Sidebar>
      <main className="workspace">
        <header className="topbar">
          <div className="top-context">
            <SidebarTrigger aria-label="메뉴 열기" />
            <span>나의 기록</span>
            <span className="slash">/</span>
            <strong>{title}</strong>
          </div>
          <div className="account-area">
            <span className="top-date">
              {localDate().replaceAll("-", ".")} ·{" "}
              {demo
                ? "체험 공간"
                : error
                  ? "동기화 실패"
                  : busy
                    ? "저장 중"
                    : loading || cached
                      ? "최신 기록 확인 중"
                      : "동기화됨"}
            </span>
            {!demo && nickname && (
              <span className="account-email" title={nickname}>
                {nickname}
              </span>
            )}
            <button
              className="logout-button"
              disabled={busy}
              aria-label={demo ? "체험 종료" : "로그아웃"}
              onClick={() =>
                guard(async () => {
                  try {
                    await onExit();
                  } catch (e) {
                    toast.error(message(e));
                  }
                })
              }
            >
              <LogOut size={17} />
            </button>
          </div>
        </header>
        {demo && (
          <div className="demo-banner">
            <FlaskConical size={16} />
            체험 중입니다. 실제 기록에 영향을 주지 않습니다.
          </div>
        )}
        {error && (
          <div className="error-banner" role="alert">
            {error}
            <button onClick={() => setRefresh((n) => n + 1)}>
              다시 불러오기
            </button>
          </div>
        )}
        <div
          className={
            "main-grid" +
            (view === "home" ? " is-home" : "") +
            (selected ? " has-detail" : "")
          }
        >
          <section className="list-section">
            <div className="page-heading">
              <div>
                <p className="eyebrow">MY MEMORY</p>
                <h1>
                  {view === "home"
                    ? "머릿속은 가볍게, 중요한 일은 또렷하게"
                    : title}
                  {view !== "home" && <span>{total}</span>}
                </h1>
              </div>
            </div>
            <p className="composer-caption">
              머릿속 할 일, 여기에 내려놓으세요.
            </p>
            <div className="quick-composer" role="group" aria-label="메모 작성">
              <div className="quick-entry">
                <textarea
                  ref={quickRef}
                  rows={1}
                  aria-label="빠른 메모"
                  placeholder="무엇을 기억해야 하나요?"
                  maxLength={10000}
                  value={quick.content}
                  disabled={blocked}
                  onChange={(e) =>
                    setQuick((q) => ({ ...q, content: e.target.value }))
                  }
                  onCompositionStart={() => (composing.current = true)}
                  onCompositionEnd={() => (composing.current = false)}
                  onKeyDown={(e) => {
                    if (
                      shouldSubmitMemo(
                        {
                          key: e.key,
                          shiftKey: e.shiftKey,
                          isComposing: e.nativeEvent.isComposing,
                          keyCode: e.keyCode,
                        },
                        composing.current,
                      )
                    ) {
                      e.preventDefault();
                      void addMemo();
                    }
                  }}
                />
              </div>
              <div className="quick-footer">
                <div className="quick-options">
                  <CategoryPriority
                    value={quick}
                    onChange={setQuick}
                    categories={categories}
                    quick
                  />
                  <button
                    className={
                      "composer-schedule-button" +
                      (quick.startDate || quick.repeatType !== "NONE"
                        ? " is-configured"
                        : "")
                    }
                    aria-expanded={expanded}
                    onClick={() => {
                      if (expanded) setQuick(clearSchedule);
                      setExpanded(!expanded);
                    }}
                  >
                    <Plus size={16} className={expanded ? "is-open" : ""} />
                    <span>일정·반복여부</span>
                  </button>
                </div>
                <button
                  className="primary"
                  disabled={blocked || !quick.content.trim()}
                  onClick={() => void addMemo()}
                >
                  추가 <span>↵</span>
                </button>
              </div>
              {expanded && (
                <div className="quick-schedule">
                  <ScheduleFields value={quick} onChange={setQuick} />
                  <button
                    className="schedule-reset"
                    onClick={() => setQuick(clearSchedule)}
                  >
                    일정·반복 초기화
                  </button>
                </div>
              )}
            </div>
            {cached && (
              <p className="schedule-hint">
                이 기기의 이전 기록을 표시하고 있습니다. 서버 기록을 확인하는
                동안 편집은 잠시 기다려 주세요.
              </p>
            )}
            {loading && (
              <p className="loading-indicator" role="status">
                <LoaderCircle className="spin" size={16} />
                기억을 불러오는 중…
              </p>
            )}
            {view === "home" ? (
              <div className="home-dashboard">
                <div className="home-main">
                  <section className="home-card home-attention">
                    <header className="home-section-heading">
                      <div>
                        <span className="home-kicker">
                          <Sun size={15} />
                          먼저 확인해요
                        </span>
                        <h2>
                          지금 챙길 기억 <span>{attentionMemos.length}</span>
                        </h2>
                      </div>
                      <span className="home-today">
                        {new Date(now).getMonth() + 1}월{" "}
                        {new Date(now).getDate()}일
                      </span>
                    </header>
                    {attentionMemos.length ? (
                      attentionMemos
                        .slice(0, showAllAttention ? undefined : 6)
                        .map(({ memo }) => row(memo, true, true))
                    ) : (
                      <div className="home-empty">
                        <Sun size={28} />
                        <strong>지금 급하게 챙길 기억은 없어요.</strong>
                        <p>오늘 할 일이나 중요한 메모를 남겨보세요.</p>
                      </div>
                    )}
                    {attentionMemos.length > 6 && (
                      <button
                        className="home-more"
                        onClick={() => setShowAllAttention(!showAllAttention)}
                      >
                        {showAllAttention ? "접기" : "챙길 기억 더 보기"}
                      </button>
                    )}
                  </section>
                  <section className="home-card">
                    <header className="home-section-heading">
                      <div>
                        <span className="home-kicker">
                          <Clock3 size={15} />
                          방금 떠오른 생각들
                        </span>
                        <h2>최근 남긴 기억</h2>
                      </div>
                      <button
                        className="home-link"
                        onClick={() => navigate("all")}
                      >
                        전체 보기
                        <ArrowUpRight size={16} />
                      </button>
                    </header>
                    {homeMemos.length ? (
                      homeMemos.slice(0, 5).map((m) => row(m, true))
                    ) : (
                      <div className="home-empty">
                        <NotebookPen size={28} />
                        <strong>첫 번째 기억을 남겨보세요.</strong>
                        <p>위 입력창에 내용만 적으면 됩니다.</p>
                      </div>
                    )}
                  </section>
                </div>
                <section className="home-card home-categories">
                  <header className="home-section-heading">
                    <div>
                      <span className="home-kicker">나만의 분류</span>
                      <h2>나의 카테고리</h2>
                    </div>
                  </header>
                  <div className="home-category-grid">
                    {categoryTree(categories).map((c, i) => (
                      <button
                        className="home-category"
                        key={c.id}
                        onClick={() => navigate(`cat:${c.id}`)}
                      >
                        <span className={"category-color c" + (i % 6)}>#</span>
                        <span className="home-category-name">
                          {categoryPath(categories, c.id)
                            .map((c) => c.name)
                            .join(" › ")}
                        </span>
                        <strong>{c.memoCount ?? 0}</strong>
                        <ArrowUpRight size={15} />
                      </button>
                    ))}
                    <button
                      className="home-category"
                      onClick={() => navigate("uncategorized")}
                    >
                      <span className="category-color">#</span>
                      <span className="home-category-name">미분류</span>
                      <strong>{counts.uncategorized ?? 0}</strong>
                      <ArrowUpRight size={15} />
                    </button>
                  </div>
                  <p className="home-category-note">
                    카테고리를 눌러 기억을 모아 보세요.
                  </p>
                </section>
              </div>
            ) : (
              <>
                <div className="list-toolbar">
                  <label className="search">
                    <Search size={17} />
                    <input
                      aria-label="메모 검색"
                      placeholder="메모 검색"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                    />
                  </label>
                  <Choice
                    label="정렬"
                    value={sort}
                    onChange={(v) => {
                      setSort(v as SortType);
                      setPage(0);
                      endDrag();
                    }}
                    options={[
                      ["CUSTOM", "커스텀"],
                      ["PRIORITY", "중요도순"],
                      ["LATEST", "최신순"],
                    ]}
                  />
                </div>
                <div className="list-label">
                  <span>
                    {view === "done" ? "완료한 기록" : "기억할 것"}
                    {sort === "CUSTOM" && (
                      <span className="custom-hint">
                        드래그로 순서 변경 · 터치는 꾹 누르기
                      </span>
                    )}
                  </span>
                  <span>중요도 · 작성 일자</span>
                </div>
                {view === "done" && (
                  <p className="schedule-hint">
                    일반 메모의 완료 목록입니다. 반복 완료 기록은 메모 상세에서
                    확인할 수 있습니다.
                  </p>
                )}
                <div className="task-list" aria-busy={loading}>
                  {memos.map((m) => row(m))}
                  {!loading && !memos.length && (
                    <div className="home-empty">
                      <Inbox size={32} />
                      <strong>
                        {keyword ? "검색 결과가 없어요" : "아직 항목이 없어요"}
                      </strong>
                      <p>
                        {keyword
                          ? "다른 단어로 검색해 보세요."
                          : "떠오르는 기억을 위 입력창에 남겨보세요."}
                      </p>
                    </div>
                  )}
                </div>
                {(page > 0 || hasNext) && (
                  <div className="pagination">
                    <button
                      className="outline"
                      disabled={page === 0 || loading || busy}
                      onClick={() => setPage((p) => p - 1)}
                    >
                      이전
                    </button>
                    <span>
                      {page + 1} 페이지 · {total}개
                    </span>
                    <button
                      className="outline"
                      disabled={!hasNext || loading || busy}
                      onClick={() => setPage((p) => p + 1)}
                    >
                      다음
                    </button>
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </main>
      <Sheet
        open={!!selected}
        modal={mobile}
        onOpenChange={(open) => {
          if (!open) closeDetail();
        }}
      >
        <SheetContent
          className="detail-sheet"
          showCloseButton={false}
          onOpenAutoFocus={(e) => e.preventDefault()}
          onInteractOutside={(e) => {
            if (!mobile) e.preventDefault();
          }}
          onTouchStart={(e) => {
            swipe.current = null;
            if (
              (e.target as Element).closest(
                'input,textarea,button,[role="combobox"]',
              )
            )
              return;
            swipe.current = {
              x: e.touches[0].clientX,
              y: e.touches[0].clientY,
            };
          }}
          onTouchEnd={(e) => {
            const start = swipe.current;
            swipe.current = null;
            if (!mobile || !start || !e.changedTouches.length) return;
            const dx = e.changedTouches[0].clientX - start.x,
              dy = e.changedTouches[0].clientY - start.y;
            if (dx > 90 && Math.abs(dy) < 50) closeDetail();
          }}
        >
          <div className="detail-top">
            <div>
              <SheetTitle>상세 정보</SheetTitle>
              <SheetDescription>
                {mobile ? "오른쪽으로 밀어 닫기" : "기억을 차분히 정리하세요"}
              </SheetDescription>
            </div>
            <button
              aria-label="상세 정보 닫기"
              className="detail-close"
              disabled={busy}
              onClick={closeDetail}
            >
              <X size={17} />
            </button>
          </div>
          {selected && draft && (
            <div className="editor">
              <fieldset disabled={blocked}>
                <label className="memo-content-field">
                  내용
                  <textarea
                    aria-label="메모 내용"
                    maxLength={10000}
                    value={draft.content}
                    onChange={(e) =>
                      setDraft({ ...draft, content: e.target.value })
                    }
                  />
                </label>
                <label
                  className={
                    "memo-notes-field" +
                    (draft.notes?.trim() ? " has-value" : "")
                  }
                >
                  <span className="memo-notes-label">
                    추가 메모 <span className="optional-label">(선택)</span>
                  </span>
                  <textarea
                    aria-label="추가 메모"
                    placeholder="설명, 준비할 것, 링크 등을 자유롭게 적어보세요."
                    maxLength={10000}
                    value={draft.notes ?? ""}
                    onChange={(e) =>
                      setDraft({ ...draft, notes: e.target.value })
                    }
                  />
                </label>
                <div className="detail-fields">
                  <CategoryPriority
                    value={draft}
                    onChange={setDraft}
                    categories={categories}
                  />
                </div>
                <ScheduleFields value={draft} onChange={setDraft} />
                <button
                  className="schedule-reset"
                  onClick={() => setDraft(clearSchedule(draft))}
                >
                  일정·반복 초기화
                </button>
                <div className="memo-timestamps">
                  <p>
                    작성일시 <time>{exactTime(selected.createdAt)}</time>
                  </p>
                  <p>
                    수정일시 <time>{exactTime(selected.updatedAt)}</time>
                  </p>
                </div>
                <button
                  className="primary save"
                  disabled={!draft.content.trim()}
                  onClick={() => void saveMemo()}
                >
                  변경 저장
                </button>
                <button className="delete" onClick={() => deleteMemo(selected)}>
                  <Trash2 size={16} />
                  메모 삭제
                </button>
              </fieldset>
              {selected.repeatType !== "NONE" && (
                <section className="history">
                  <div>
                    <h3>반복 완료 기록</h3>
                    <button
                      type="button"
                      aria-expanded={historyExpanded}
                      onClick={() => {
                        setHistoryPage(0);
                        setHistoryExpanded(!historyExpanded);
                      }}
                    >
                      {historyExpanded ? "접기" : "더보기"}
                    </button>
                  </div>
                  {historyExpanded && (historyLoading ? (
                    <p role="status">기록을 불러오는 중…</p>
                  ) : (
                    <>
                      {historyError && (
                        <p role="alert">
                          {historyError}
                          <button
                            onClick={() => void loadHistory(selected.id, historyPage)}
                          >
                            다시 조회
                          </button>
                        </p>
                      )}
                      {!historyLoading && !historyError && !history.length && (
                        <p>완료 기록이 없습니다.</p>
                      )}
                      {history.map((h) => (
                        <div key={h.scheduledDate}>
                          <span>
                            {h.scheduledDate}
                            <small>{exactTime(h.completedAt)}</small>
                          </span>
                          <button
                            disabled={busy}
                            onClick={() =>
                              void perform(async () => {
                                await client.undoHistory(
                                  selected.id,
                                  h.scheduledDate,
                                );
                                await loadHistory(selected.id, historyPage);
                              }, "완료를 취소했습니다.")
                            }
                          >
                            완료 취소
                          </button>
                        </div>
                      ))}
                      <div className="history-paging">
                        <button
                          disabled={historyPage === 0 || busy}
                          onClick={() => setHistoryPage((p) => p - 1)}
                        >
                          이전
                        </button>
                        <button
                          disabled={!historyMore || busy}
                          onClick={() => setHistoryPage((p) => p + 1)}
                        >
                          다음
                        </button>
                      </div>
                    </>
                  ))}
                </section>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
      <Dialog
        open={!!categoryEditor}
        onOpenChange={(open) => {
          if (!open && !busy) setCategoryEditor(null);
        }}
      >
        <DialogContent>
          <DialogTitle>
            {categoryEditor?.id ? "카테고리 편집" : "카테고리 추가"}
          </DialogTitle>
          <DialogDescription>
            이름과 상위 카테고리를 선택하세요.
          </DialogDescription>
          {categoryEditor && (
            <form
              key={categoryEditor.id ?? "new"}
              className="category-form"
              onSubmit={(e) => {
                e.preventDefault();
                void saveCategory();
              }}
            >
              <label>
                이름
                <input
                  autoFocus
                  className="text-field"
                  required
                  maxLength={100}
                  value={categoryEditor.name}
                  onChange={(e) =>
                    setCategoryEditor({
                      ...categoryEditor,
                      name: e.target.value,
                    })
                  }
                  onKeyDown={(e) => {
                    if (
                      e.key === "Enter" &&
                      (e.nativeEvent.isComposing || e.keyCode === 229)
                    )
                      e.preventDefault();
                  }}
                />
              </label>
              <label>
                상위 카테고리
                <Choice
                  label="상위 카테고리"
                  value={String(categoryEditor.parentId || 0)}
                  onChange={(v) =>
                    setCategoryEditor({
                      ...categoryEditor,
                      parentId: Number(v) || null,
                    })
                  }
                  options={[
                    ["0", "최상위"],
                    ...categoryTree(categories)
                      .filter(
                        (c) =>
                          !categoryPath(categories, c.id).some(
                            (p) => p.id === categoryEditor.id,
                          ),
                      )
                      .map(
                        (c) =>
                          [
                            String(c.id),
                            categoryPath(categories, c.id)
                              .map((p) => p.name)
                              .join(" › "),
                          ] as [string, string],
                      ),
                  ]}
                />
              </label>
              {categoryEditor.id !== null && (
                <button
                  type="button"
                  className="outline"
                  disabled={busy}
                  onClick={() =>
                    setCategoryEditor({ id: null, name: "", parentId: categoryEditor.id })
                  }
                >
                  하위 카테고리 추가
                </button>
              )}
              <div className="dialog-actions">
                {categoryEditor.id !== null && (
                  <button
                    type="button"
                    className="category-delete"
                    disabled={busy}
                    onClick={() =>
                      deleteCategory(
                        categories.find((c) => c.id === categoryEditor.id)!,
                      )
                    }
                  >
                    <Trash2 size={16} aria-hidden="true" />
                    삭제
                  </button>
                )}
                <button
                  type="button"
                  className="outline"
                  disabled={busy}
                  onClick={() => setCategoryEditor(null)}
                >
                  취소
                </button>
                <button className="primary" disabled={busy}>
                  저장
                </button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
      <Dialog
        open={!!confirmation}
        onOpenChange={(open) => {
          if (!open && !busy) setConfirmation(null);
        }}
      >
        <DialogContent showCloseButton={false}>
          <DialogTitle>{confirmation?.title}</DialogTitle>
          <DialogDescription className="confirm-body">
            {confirmation?.body}
          </DialogDescription>
          <div className="dialog-actions">
            <button
              autoFocus
              className="outline"
              disabled={busy}
              onClick={() => setConfirmation(null)}
            >
              취소
            </button>
            <button
              className={confirmation?.danger ? "danger-button" : "primary"}
              disabled={busy}
              onClick={async () => {
                const action = confirmation?.action;
                setConfirmation(null);
                await action?.();
              }}
            >
              {confirmation?.label}
            </button>
          </div>
        </DialogContent>
      </Dialog>
      <Dialog open={settings} onOpenChange={setSettings}>
        <DialogContent>
          <DialogTitle>설정</DialogTitle>
          <DialogDescription>기억함의 저장 및 계정 안내</DialogDescription>
          <p className="storage-info">
            {demo
              ? "체험 데이터는 이 화면에서만 유지됩니다. 실제 계정에는 저장되지 않습니다."
              : "기록은 로그인한 계정에 저장됩니다. 인터넷에 연결된 상태에서 이용해 주세요. 빠른 시작을 위한 사본은 이 브라우저에 암호화되어 최대 24시간 보관됩니다. 로그아웃·세션 만료 시 사본과 키를 삭제합니다. 다른 기기로 동기화되는 원본은 서버에 있습니다."}
          </p>
          <button
            className="outline"
            onClick={() =>
              guard(async () => {
                try {
                  await onExit();
                } catch (e) {
                  toast.error(message(e));
                }
              })
            }
          >
            {demo ? "체험 종료" : "로그아웃"}
          </button>
        </DialogContent>
      </Dialog>
    </SidebarProvider>
  );
}
