import type { MemoInput, Category, RepeatType, Priority } from "@/lib/model";
import {
  blankMemo,
  categoryPath,
  categoryTree,
  days,
  localDate,
  priorities,
} from "@/lib/model";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
export function Choice({
  label,
  value,
  onChange,
  options,
  disabled = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: [string, string][];
  disabled?: boolean;
}) {
  return (
    <Select value={value} onValueChange={onChange} disabled={disabled}>
      <SelectTrigger aria-label={label}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {options.map(([id, name]) => (
          <SelectItem key={id} value={id}>
            {name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
export function CategoryPriority({
  value,
  onChange,
  categories,
  quick = false,
}: {
  value: MemoInput;
  onChange: (value: MemoInput) => void;
  categories: Category[];
  quick?: boolean;
}) {
  const categoryField = (
    <div className={!quick || value.content.trim() ? "has-value" : ""}>
      <span>카테고리</span>
      <Choice
        label={quick ? "빠른 입력 카테고리" : "메모 카테고리"}
        value={String(value.categoryId || 0)}
        onChange={(v) => onChange({ ...value, categoryId: Number(v) || null })}
        options={[
          ["0", "미분류"],
          ...categoryTree(categories).map(
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
    </div>
  );
  const priorityField = (
    <div className={!quick || value.content.trim() ? "has-value" : ""}>
      <span>중요도</span>
      <Choice
        label={quick ? "빠른 입력 중요도" : "메모 중요도"}
        value={value.priority}
        onChange={(v) => onChange({ ...value, priority: v as Priority })}
        options={priorities}
      />
    </div>
  );
  return quick ? (
    <>
      {categoryField}
      {priorityField}
    </>
  ) : (
    <>
      {priorityField}
      {categoryField}
    </>
  );
}
export function clearSchedule(value: MemoInput): MemoInput {
  const blank = blankMemo();
  return {
    ...value,
    startDate: blank.startDate,
    endDate: null,
    startTime: null,
    endTime: null,
    repeatType: "NONE",
    repeatDays: [],
    repeatDayOfMonth: null,
    repeatMonth: null,
  };
}
export function ScheduleFields({
  value,
  onChange,
}: {
  value: MemoInput;
  onChange: (value: MemoInput) => void;
}) {
  const patch = (p: Partial<MemoInput>) => onChange({ ...value, ...p });
  const timed = !!(value.startTime || value.endTime);
  const range = !!value.endDate;
  const changeRepeat = (repeatType: RepeatType) =>
    patch({
      repeatType,
      repeatDays: repeatType === "WEEKLY" ? value.repeatDays : [],
      repeatDayOfMonth: ["MONTHLY", "YEARLY"].includes(repeatType)
        ? value.repeatDayOfMonth || new Date().getDate()
        : null,
      repeatMonth:
        repeatType === "YEARLY"
          ? value.repeatMonth || new Date().getMonth() + 1
          : null,
    });
  return (
    <>
      <section
        className={"schedule-card" + (value.startDate ? " is-enabled" : "")}
        aria-label="일정 설정"
      >
        <div className="schedule-card-heading">
          <div className="schedule-heading-control">
            <span>일정</span>{" "}
            <label className="schedule-switch">
              <input
                type="checkbox"
                aria-label="일자 지정"
                checked={!!value.startDate}
                onChange={(e) =>
                  patch(
                    e.target.checked
                      ? { startDate: localDate() }
                      : {
                          startDate: null,
                          endDate: null,
                          startTime: null,
                          endTime: null,
                        },
                  )
                }
              />
              <span className="schedule-switch-track" />
            </label>
          </div>
          <span className="schedule-summary">
            {value.startDate
              ? range
                ? "기간 일정"
                : "하루 일정"
              : "날짜 없이 보관"}
          </span>
        </div>
        <div className="schedule-switches">
          {value.startDate && (
            <>
              <label className="schedule-switch">
                <input
                  type="checkbox"
                  checked={range}
                  onChange={(e) =>
                    patch({
                      endDate: e.target.checked ? value.startDate : null,
                      endTime:
                        e.target.checked && timed ? value.startTime : null,
                    })
                  }
                />
                <span className="schedule-switch-track" />
                <span>기간으로 설정</span>
              </label>
              <label className="schedule-switch">
                <input
                  type="checkbox"
                  checked={timed}
                  onChange={(e) =>
                    patch(
                      e.target.checked
                        ? {
                            startTime: "09:00",
                            endTime: range ? "10:00" : null,
                          }
                        : { startTime: null, endTime: null },
                    )
                  }
                />
                <span className="schedule-switch-track" />
                <span>시간 지정</span>
              </label>
            </>
          )}
        </div>
        {value.startDate && (
          <div className="schedule-date-grid">
            <label>
              시작일
              <input
                aria-label="시작일"
                type="date"
                value={value.startDate}
                required
                onChange={(e) => patch({ startDate: e.target.value || null })}
              />
            </label>
            {range && (
              <label>
                종료일
                <input
                  aria-label="종료일"
                  type="date"
                  required
                  min={value.startDate}
                  value={value.endDate || ""}
                  onChange={(e) => patch({ endDate: e.target.value || null })}
                />
              </label>
            )}
            {timed && (
              <>
                <label>
                  시작 시간
                  <input
                    aria-label="시작 시간"
                    type="time"
                    required
                    value={value.startTime?.slice(0, 5) || ""}
                    onChange={(e) =>
                      patch({ startTime: e.target.value || null })
                    }
                  />
                </label>
                {range && (
                  <label>
                    종료 시간
                    <input
                      aria-label="종료 시간"
                      type="time"
                      required
                      value={value.endTime?.slice(0, 5) || ""}
                      onChange={(e) =>
                        patch({ endTime: e.target.value || null })
                      }
                    />
                  </label>
                )}
              </>
            )}
          </div>
        )}
      </section>
      <section
        className={
          "schedule-card repeat-card" +
          (value.repeatType !== "NONE" ? " is-enabled" : "")
        }
        aria-label="반복 설정"
      >
        <div className="schedule-card-heading">
          <div className="schedule-heading-control">
            <span>반복</span>{" "}
            <label className="schedule-switch">
              <input
                type="checkbox"
                aria-label="반복 설정"
                checked={value.repeatType !== "NONE"}
                onChange={(e) =>
                  changeRepeat(e.target.checked ? "DAILY" : "NONE")
                }
              />
              <span className="schedule-switch-track" />
            </label>
          </div>
          <span className="schedule-summary">
            {value.repeatType === "NONE" ? "반복 없음" : "반복 설정됨"}
          </span>
        </div>
        {value.repeatType !== "NONE" && (
          <div className="repeat-options">
            <Choice
              label="반복 주기"
              value={value.repeatType}
              onChange={(v) => changeRepeat(v as RepeatType)}
              options={[
                ["DAILY", "매일"],
                ["WEEKLY", "매주"],
                ["MONTHLY", "매월"],
                ["YEARLY", "매년"],
              ]}
            />
            {value.repeatType === "WEEKLY" && (
              <div className="weekdays">
                {days.map(([id, name]) => (
                  <label key={id}>
                    <input
                      type="checkbox"
                      checked={value.repeatDays.includes(id)}
                      onChange={(e) =>
                        patch({
                          repeatDays: e.target.checked
                            ? [...value.repeatDays, id]
                            : value.repeatDays.filter((d) => d !== id),
                        })
                      }
                    />
                    {name}
                  </label>
                ))}
              </div>
            )}
            {["MONTHLY", "YEARLY"].includes(value.repeatType) && (
              <div className="schedule-date-grid">
                {value.repeatType === "YEARLY" && (
                  <label>
                    월
                    <input
                      aria-label="반복 월"
                      type="number"
                      min={1}
                      max={12}
                      value={value.repeatMonth || ""}
                      onChange={(e) =>
                        patch({ repeatMonth: Number(e.target.value) || null })
                      }
                    />
                  </label>
                )}
                <label>
                  일
                  <input
                    aria-label="반복 일"
                    type="number"
                    min={1}
                    max={31}
                    value={value.repeatDayOfMonth || ""}
                    onChange={(e) =>
                      patch({
                        repeatDayOfMonth: Number(e.target.value) || null,
                      })
                    }
                  />
                </label>
              </div>
            )}
            <p className="schedule-hint">
              일정과 별도로 반복합니다.
              {["MONTHLY", "YEARLY"].includes(value.repeatType) &&
                " 해당 일자가 없는 달에는 마지막 날에 표시됩니다."}
            </p>
          </div>
        )}
      </section>
    </>
  );
}
