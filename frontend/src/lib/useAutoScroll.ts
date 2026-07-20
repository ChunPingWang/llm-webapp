import { useEffect, useRef } from "react";

/** 判定「貼底」的容忍距離(px):在此範圍內視為使用者停在底部,內容更新時自動跟隨。 */
const STICK_THRESHOLD = 80;

/**
 * 串流內容自動捲動:deps 變化時將容器捲至底部,讓使用者即時看到 LLM 回應。
 * 使用者往上捲動(離底超過門檻)即暫停跟隨,捲回底部後恢復,不搶奪捲動控制權。
 */
export function useAutoScroll<T extends HTMLElement>(deps: readonly unknown[]) {
  const ref = useRef<T>(null);
  const stick = useRef(true);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const onScroll = () => {
      stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < STICK_THRESHOLD;
    };
    el.addEventListener("scroll", onScroll);
    return () => el.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    const el = ref.current;
    if (el && stick.current) {
      el.scrollTop = el.scrollHeight;
    }
  }, deps);

  return ref;
}
