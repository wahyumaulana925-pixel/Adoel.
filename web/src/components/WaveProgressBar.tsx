import { useEffect, useRef } from "react";
import { useElementWidth } from "../hooks/useElementWidth";

const WAVELENGTH_PX = 18;
const STEP_PX = 2;
const FLOW_PERIOD_MS = 1200;

/** Bar progress track-and-fill dengan isian berbentuk gelombang sinus yang terus mengalir
 * (seperti air melalui talang), bukan bar flat — port 1:1 dari LinearProgressBar di
 * ProgressBar.kt (aplikasi Android). RadarCard memakai animated=true (satu-satunya elemen
 * yang terus bergerak di kartunya); header shift-progress & baris shift Statistik memakai
 * animated=false (bentuk gelombang sama, tapi diam). */
export function WaveProgressBar({
  fraction,
  trackColor,
  fillColor,
  height = 4,
  cornerRadius = 2,
  animated = true,
  width,
}: {
  fraction: number;
  trackColor: string;
  fillColor: string;
  height?: number;
  cornerRadius?: number;
  animated?: boolean;
  width?: number;
}) {
  const { ref, width: measuredWidth } = useElementWidth<HTMLDivElement>(width);
  const clamped = Math.min(1, Math.max(0, fraction));
  const progressWidth = measuredWidth * clamped;
  const amplitude = height * 0.5;
  const strokeWidth = height * 0.6;
  const midY = height / 2;
  const pathWidth = progressWidth + WAVELENGTH_PX;

  let d = "";
  if (progressWidth > 0.5) {
    const pts: string[] = [];
    for (let x = 0; x <= pathWidth; x += STEP_PX) {
      const y = midY + amplitude * Math.sin((2 * Math.PI * x) / WAVELENGTH_PX);
      pts.push(`${x.toFixed(1)},${y.toFixed(1)}`);
    }
    d = "M" + pts.join(" L");
  }

  const rollDiameter = height * (1.6 + 1.4 * clamped);
  // Fase di titik progressWidth pada t=0 — dipakai sebagai titik tolak diam (baseline) di
  // bawah, lalu di-drive lanjut lewat rAF di bawah supaya bola betul-betul menunggangi
  // puncak gelombang yang mengalir di baliknya (bukan bobbing sendiri lepas dari gelombang).
  const rollBasePhase = (2 * Math.PI * progressWidth) / WAVELENGTH_PX;
  const rollY = midY + amplitude * Math.sin(rollBasePhase);
  const rollRef = useRef<HTMLDivElement>(null);

  // Gelombang sendiri mengalir murni lewat CSS transform (lihat .wave-svg-animated) —
  // ringan, tidak menghitung ulang path tiap frame. Tapi itu artinya posisi Y bola di
  // ujung fill TIDAK otomatis ikut terhitung ulang oleh CSS itu — sebelumnya bola cuma
  // diam di posisi awal + wobble generik ±1px yang tidak selaras dengan gelombang yang
  // benar-benar lewat di baliknya, jadi ikut kelihatan berantakan. rAF di sini menghitung
  // fase yang identik dengan translateX gelombang (satu wavelength = satu putaran fase
  // penuh per FLOW_PERIOD_MS), lalu menulis delta-Y-nya langsung ke DOM lewat ref (bukan
  // lewat state) supaya tidak memicu re-render React tiap frame.
  useEffect(() => {
    if (!animated || progressWidth <= 2) return;
    let raf = 0;
    const start = performance.now();
    function tick(now: number) {
      const elapsed = now - start;
      const flowPhase = ((elapsed % FLOW_PERIOD_MS) / FLOW_PERIOD_MS) * 2 * Math.PI;
      const y = amplitude * Math.sin(rollBasePhase - flowPhase);
      const baseline = amplitude * Math.sin(rollBasePhase);
      if (rollRef.current) rollRef.current.style.transform = `translateY(${(y - baseline).toFixed(2)}px)`;
      raf = requestAnimationFrame(tick);
    }
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [animated, progressWidth, amplitude, rollBasePhase]);

  return (
    <div
      ref={ref}
      className="wave-track"
      style={{ width: width != null ? width : "100%", height }}
    >
      <div className="wave-track-bg" style={{ borderRadius: cornerRadius, background: trackColor }} />
      {progressWidth > 0.5 && (
        <div className="wave-fill-clip" style={{ width: progressWidth, height, borderRadius: cornerRadius }}>
          <svg
            className={animated ? "wave-svg wave-svg-animated" : "wave-svg"}
            style={{ width: pathWidth, height, ["--wave-wavelength" as any]: `${WAVELENGTH_PX}px` }}
            viewBox={`0 0 ${pathWidth} ${height}`}
          >
            <path d={d} fill="none" stroke={fillColor} strokeWidth={strokeWidth} strokeLinecap="round" />
          </svg>
        </div>
      )}
      {progressWidth > 2 && (
        <div
          ref={rollRef}
          className="wave-roll"
          style={{
            width: rollDiameter,
            height: rollDiameter,
            left: progressWidth - rollDiameter / 2,
            top: rollY - rollDiameter / 2,
            background: `linear-gradient(90deg, color-mix(in srgb, ${fillColor} 65%, black), color-mix(in srgb, ${fillColor} 60%, white), color-mix(in srgb, ${fillColor} 65%, black))`,
          }}
        />
      )}
    </div>
  );
}
