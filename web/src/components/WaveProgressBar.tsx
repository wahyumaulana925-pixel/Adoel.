import { useElementWidth } from "../hooks/useElementWidth";

/** Bar progress track-and-fill lurus (plain) — port 1:1 dari LinearProgressBar di
 * ProgressBar.kt (aplikasi Android). Fill tumbuh dengan transisi width via CSS. */
export function WaveProgressBar({
  fraction,
  trackColor,
  fillColor,
  height = 4,
  cornerRadius = 2,
  width,
}: {
  fraction: number;
  trackColor: string;
  fillColor: string;
  height?: number;
  cornerRadius?: number;
  width?: number;
}) {
  const { ref, width: measuredWidth } = useElementWidth<HTMLDivElement>(width);
  const clamped = Math.min(1, Math.max(0, fraction));
  const progressWidth = measuredWidth * clamped;

  return (
    <div
      ref={ref}
      className="wave-track"
      style={{ width: width != null ? width : "100%", height, borderRadius: cornerRadius, overflow: "hidden" }}
    >
      <div className="wave-track-bg" style={{ background: trackColor }} />
      {progressWidth > 0.5 && (
        <div className="wave-fill" style={{ width: progressWidth, height, background: fillColor }} />
      )}
    </div>
  );
}
