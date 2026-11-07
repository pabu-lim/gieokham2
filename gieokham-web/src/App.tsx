import { useEffect, useRef, useState } from "react";
import { ArrowUpRight, FlaskConical, LogIn, LoaderCircle } from "lucide-react";
import { Toaster, toast } from "sonner";
import { InstallApp } from "@/components/install-app";
import { Workspace } from "@/components/workspace";
import { api, ApiError, resetRequests } from "@/lib/api";
import { clearEncryptedCache } from "@/lib/encrypted-cache";
import { createDemoApi } from "@/lib/demo";
import type { Member } from "@/lib/model";
const message = (error: unknown) =>
  error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
const aborted = (error: unknown) =>
  error instanceof DOMException && error.name === "AbortError";

export default function App() {
  const [member, setMember] = useState<Member | null>(null),
    [checking, setChecking] = useState(true),
    [authError, setAuthError] = useState(""),
    [demo, setDemo] = useState<ReturnType<typeof createDemoApi> | null>(null);
  const [name, setName] = useState(""),
    [nickname, setNickname] = useState(""),
    [busy, setBusy] = useState(false);
  const authChannel = useRef<BroadcastChannel | null>(null);
  const authVersion = useRef(0),
    accountId = useRef<number | null>(null);
  const clear = (notify = false) => {
    if (notify) authChannel.current?.postMessage("logout");
    accountId.current = null;
    void clearEncryptedCache().catch(() => {});
    authVersion.current++;
    resetRequests();
    setMember(null);
    setName("");
    setNickname("");
    setAuthError("");
    setChecking(false);
    toast.dismiss();
  };
  async function check() {
    const version = ++authVersion.current;
    try {
      const next = await api.me();
      if (version !== authVersion.current) return;
      if (
        accountId.current !== null &&
        (!next.registered || next.memberId !== accountId.current)
      ) {
        resetRequests();
        void clearEncryptedCache().catch(() => {});
      }
      accountId.current = next.registered ? next.memberId : null;
      setMember(next);
      setAuthError("");
      if (!next.registered) {
        setName(next.name);
        setNickname(next.name);
      }
    } catch (e) {
      if (version !== authVersion.current) return;
      if (!(e instanceof ApiError && e.status === 401) && !aborted(e))
        setAuthError(message(e));
    } finally {
      if (version === authVersion.current) setChecking(false);
    }
  }
  useEffect(() => {
    void check();
    const expired = () => clear(true);
    const channel =
      typeof BroadcastChannel !== "undefined"
        ? new BroadcastChannel("gieokham2-auth")
        : null;
    authChannel.current = channel;
    if (channel) channel.onmessage = () => clear();
    const focus = () => {
      if (document.visibilityState === "visible") void check();
    };
    window.addEventListener("gieokham:unauthorized", expired);
    document.addEventListener("visibilitychange", focus);
    return () => {
      channel?.close();
      authChannel.current = null;
      authVersion.current++;
      window.removeEventListener("gieokham:unauthorized", expired);
      document.removeEventListener("visibilitychange", focus);
    };
  }, []);
  const logout = async () => {
    await api.logout();
    clear(true);
  };
  const enterDemo = () => {
    resetRequests();
    setDemo(createDemoApi());
  };
  async function signup() {
    if (busy) return;
    setBusy(true);
    try {
      const result = await api.signup(name, nickname);
      setMember(result);
    } catch (e) {
      toast.error(message(e));
    } finally {
      setBusy(false);
    }
  }
  let content;
  if (demo)
    content = (
      <Workspace
        key="demo"
        client={demo}
        demo
        onExit={() => {
          setDemo(null);
          void check();
        }}
        onDemo={() => {}}
      />
    );
  else if (checking)
    content = (
      <main className="auth-page">
        <section className="auth-card auth-loading">
          <LoaderCircle className="spin" />
          <p>기억함을 여는 중입니다.</p>
        </section>
      </main>
    );
  else if (member?.registered)
    content = (
      <Workspace
        key={member.memberId}
        client={api}
        memberId={member.memberId}
        nickname={member.nickname}
        demo={false}
        onExit={logout}
        onDemo={enterDemo}
      />
    );
  else
    content = (
      <main className="auth-page">
        <section className="auth-card">
          <span className="auth-logo">
            <img src="/icons/memo-cutout-192.png" alt="" />
          </span>
          <p className="eyebrow">GIEOKHAM</p>
          <h1>
            {member ? "기억함에 오신 것을 환영해요" : "기억할 것은 기억함에"}
          </h1>
          <p className="auth-copy">
            떠오른 메모와 오늘 할 일을 빠르게 남기고
            <br />
            중요한 일에 집중하세요.
          </p>
          {member && !member.registered ? (
            <form
              className="signup-form"
              onSubmit={(e) => {
                e.preventDefault();
                void signup();
              }}
            >
              <label>
                이름
                <input
                  className="text-field"
                  required
                  maxLength={100}
                  value={name}
                  disabled={!member.nameEditable}
                  onChange={(e) => setName(e.target.value)}
                />
              </label>
              <label>
                닉네임
                <input
                  className="text-field"
                  required
                  maxLength={100}
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                />
              </label>
              <button
                className="primary"
                disabled={busy || !name.trim() || !nickname.trim()}
              >
                {busy ? "가입 중…" : "가입하기"}
              </button>
              <button
                type="button"
                className="outline"
                disabled={busy}
                onClick={() =>
                  void logout().catch((e) => toast.error(message(e)))
                }
              >
                다른 계정으로 로그인
              </button>
            </form>
          ) : (
            <>
              <a className="google-login" href="/oauth2/authorization/google">
                <LogIn size={20} />
                Google 계정으로 계속
              </a>
              <button className="demo-button auth-demo" onClick={enterDemo}>
                <FlaskConical size={17} />
                예시로 체험하기
                <ArrowUpRight size={15} />
              </button>
            </>
          )}
          {authError && (
            <div className="error-banner" role="alert">
              {authError}
              <button onClick={() => void check()}>다시 연결</button>
            </div>
          )}
          {new URLSearchParams(location.search).has("loginError") && (
            <p role="alert" className="error-banner">
              로그인을 완료하지 못했습니다. 다시 시도해 주세요.
            </p>
          )}
          <p className="auth-note">
            내 기록은 로그인한 계정별로 분리되어 저장됩니다.
          </p>
        </section>
      </main>
    );
  return (
    <>
      {content}
      <Toaster theme="light" position="bottom-center" richColors />
      <InstallApp />
    </>
  );
}
