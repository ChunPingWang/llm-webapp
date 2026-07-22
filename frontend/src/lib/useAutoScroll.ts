import { useEffect, useRef } from "react";

/** 判定「貼底」的容忍距離(px):在此範圍內視為使用者停在底部,內容更新時自動跟隨。 */
const STICK_THRESHOLD = 80;

/**
 * 串流內容自動捲動:讓使用者即時看到 LLM 回應。
 *
 * 以 MutationObserver 監看容器 DOM 實際變化(含非同步程式碼高亮、圖片載入等
 * React 渲染後才發生的內容長高),而非僅依賴 deps 重新渲染,捲動不落後。
 * 只有「使用者主動捲動」離開底部才暫停跟隨(程式自身捲動所觸發的 scroll 事件
 * 不參與判定,避免版面位移誤判);使用者捲回底部即自動恢復。
 */
export function useAutoScroll<T extends HTMLElement>(deps: readonly unknown[]) {
  const ref = useRef<T>(null);
  const stick = useRef(true);
  const programmatic = useRef(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const toBottom = () => {
      programmatic.current = true;
      el.scrollTop = el.scrollHeight;
      requestAnimationFrame(() => {
        programmatic.current = false;
      });
    };

    const onScroll = () => {
      if (programmatic.current) return; // 程式捲動不改變跟隨狀態
      stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < STICK_THRESHOLD;
    };

    // DOM 實際變化(串流增量、非同步高亮、節點增刪)即跟隨,不受渲染時序影響
    const observer = new MutationObserver(() => {
      if (stick.current) toBottom();
    });
    observer.observe(el, { childList: true, subtree: true, characterData: true });
    el.addEventListener("scroll", onScroll);
    return () => {
      observer.disconnect();
      el.removeEventListener("scroll", onScroll);
    };
  }, []);

  // deps 變化(如切換分頁)時補一次:MutationObserver 掛在容器上,內容整批替換也涵蓋,
  // 此處確保初次掛載與同內容重掛時也捲至底部。
  useEffect(() => {
    const el = ref.current;
    if (el && stick.current) {
      programmatic.current = true;
      el.scrollTop = el.scrollHeight;
      requestAnimationFrame(() => {
        programmatic.current = false;
      });
    }
  }, deps);

  return ref;
}
