import { useEffect, useRef, useState } from "react";

/** Lebar elemen dalam px, dipantau lewat ResizeObserver — dipakai komponen yang perlu
 * menghitung path SVG dalam satuan px asli (wave progress bar, woven divider), sama
 * seperti BoxWithConstraints di Compose menyediakan maxWidth ter-resolve. */
export function useElementWidth<T extends HTMLElement>(fixedWidth?: number) {
  const ref = useRef<T>(null);
  const [width, setWidth] = useState(fixedWidth ?? 0);

  useEffect(() => {
    if (fixedWidth != null) {
      setWidth(fixedWidth);
      return;
    }
    const el = ref.current;
    if (!el) return;
    const update = () => setWidth(el.clientWidth);
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, [fixedWidth]);

  return { ref, width };
}
