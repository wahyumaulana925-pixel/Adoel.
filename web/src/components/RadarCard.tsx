import { useRef, useState, type PointerEvent as ReactPointerEvent, type ReactNode } from "react";
import { absMinToTimeStr, formatDeltaMin, formatYard } from "../domain/format";
import { effectiveRemaining, urgencyLevel, type UrgencyLevel } from "../domain/estimasiUtils";
import { TIPE_COLOR } from "../domain/mesinVisual";
import type { Estimasi, MesinData } from "../domain/types";
import { WaveProgressBar } from "./WaveProgressBar";
import { MesinTipeIcon, PauseIcon, PlayIcon, DeleteIcon } from "./Icons";

const REMINDER_LEAD_MIN = 5;
const SWIPE_THRESHOLD_PX = 88;
const SWIPE_MAX_PX = 140;
const LONG_PRESS_MS = 450;
const DRAG_INTENT_PX = 8;

type CardFace = "FRONT" | "ACTIONS" | "PAUSED";

const URGENCY_STYLE: Record<UrgencyLevel, { accent: string; bar: string; text: string; pulse: boolean }> = {
  CALM: { accent: "var(--cyan-500)", bar: "var(--cyan-500)", text: "var(--cyan-400)", pulse: false },
  SOON: { accent: "var(--amber-500)", bar: "var(--amber-400)", text: "var(--amber-400)", pulse: false },
  IMMINENT: { accent: "var(--amber-600, #d97706)", bar: "var(--amber-600, #d97706)", text: "var(--orange-400)", pulse: false },
  OVERDUE: { accent: "var(--red-500)", bar: "var(--red-500)", text: "var(--red-400, #f87171)", pulse: true },
};

/** Kartu radar — tekan-tahan membalik ke Jeda/Hapus, swipe kanan = doff normal, swipe kiri =
 * doff dengan keterangan Matching, tap zona nomor mesin/corak = ubah corak+yard, tap zona
 * waktu = ubah estimasi. Port 1:1 dari RadarCard.kt (aplikasi Android) — motion disederhanakan
 * ke transisi CSS (bukan frame-by-frame Compose), tapi struktur gestur & muka kartu sama. */
export function RadarCard({
  est,
  mesin,
  nowAbs,
  onDoff,
  onDoffMatching,
  onHapus,
  onJeda,
  onLanjutkan,
  onQuickEdit,
  onEditWaktu,
}: {
  est: Estimasi;
  mesin: MesinData | null;
  nowAbs: number;
  onDoff: () => void;
  onDoffMatching: () => void;
  onHapus: () => void;
  onJeda: () => void;
  onLanjutkan: () => void;
  onQuickEdit: () => void;
  onEditWaktu: () => void;
}) {
  const remaining = effectiveRemaining(est, nowAbs);
  const level = urgencyLevel(remaining);
  const style = URGENCY_STYLE[level];
  const totalDur = est.estAbsMin - est.startAbsMin;
  const elapsed = nowAbs - est.startAbsMin;
  const progress = totalDur > 0 ? Math.min(1, Math.max(0, elapsed / totalDur)) : 0;
  const corak = est.corakOverride ?? mesin?.corak ?? "—";
  const standardYard = est.yardOverride ?? mesin?.targetYard ?? null;
  const corakLine = standardYard != null ? `${corak} · ${formatYard(standardYard)}y` : corak;
  const showDot = remaining <= 5;
  const swipeEnabled = remaining <= REMINDER_LEAD_MIN;

  const isPaused = est.pausedAtAbsMin !== null;
  const [showActionsFace, setShowActionsFace] = useState(false);
  const face: CardFace = isPaused ? "PAUSED" : showActionsFace ? "ACTIONS" : "FRONT";

  const [offsetX, setOffsetX] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [completing, setCompleting] = useState<"NORMAL" | "MATCHING" | null>(null);
  const dragStartX = useRef<number | null>(null);
  const wasDrag = useRef(false);
  const longPressTimer = useRef<number | null>(null);

  function clearLongPress() {
    if (longPressTimer.current != null) {
      window.clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  }

  function handlePointerDown(e: ReactPointerEvent) {
    if (completing || face !== "FRONT") return;
    dragStartX.current = e.clientX;
    wasDrag.current = false;
    longPressTimer.current = window.setTimeout(() => {
      longPressTimer.current = null;
      if (!wasDrag.current) {
        navigator.vibrate?.(20);
        setShowActionsFace(true);
        setOffsetX(0);
        setDragging(false);
        dragStartX.current = null;
      }
    }, LONG_PRESS_MS);
  }

  function handlePointerMove(e: ReactPointerEvent) {
    if (dragStartX.current === null || completing || face !== "FRONT") return;
    const dx = e.clientX - dragStartX.current;
    if (Math.abs(dx) > DRAG_INTENT_PX) {
      wasDrag.current = true;
      clearLongPress();
    }
    if (swipeEnabled && wasDrag.current) {
      setDragging(true);
      setOffsetX(Math.max(-SWIPE_MAX_PX, Math.min(SWIPE_MAX_PX, dx)));
    }
  }

  function endDrag() {
    clearLongPress();
    dragStartX.current = null;
    setDragging(false);
    if (Math.abs(offsetX) >= SWIPE_THRESHOLD_PX) {
      triggerDoff(offsetX > 0 ? "NORMAL" : "MATCHING");
    } else {
      setOffsetX(0);
    }
  }

  function triggerDoff(kind: "NORMAL" | "MATCHING") {
    if (completing) return;
    setCompleting(kind);
    navigator.vibrate?.(kind === "NORMAL" ? 20 : [15, 60, 15]);
    setOffsetX(kind === "NORMAL" ? 420 : -420);
    window.setTimeout(() => {
      if (kind === "NORMAL") onDoff();
      else onDoffMatching();
    }, 260);
  }

  function handleZoneClick(action: () => void) {
    if (wasDrag.current || face !== "FRONT") return;
    action();
  }

  return (
    <div className="radar-card-outer" style={{ ["--urgency-accent" as any]: style.accent }}>
      <div
        className="radar-card-swipe"
        style={{
          transform: `translateX(${offsetX}px)`,
          transition: dragging ? "none" : "transform 0.25s cubic-bezier(0.4,0,0.2,1)",
          opacity: completing ? 0.15 : 1,
        }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
      >
        <div className={`radar-card-flipper${face !== "FRONT" ? " flipped" : ""}`}>
          <div className={`radar-card-face radar-card-front${level === "OVERDUE" ? " overdue" : ""}`}>
            <div className="radar-card-accent" />
            <div className="radar-card-body">
              <div className="radar-card-main" onClick={() => handleZoneClick(onQuickEdit)} role="button">
                <div className="radar-card-title-row">
                  <span className="radar-card-mcno">{est.mcNo}</span>
                  {mesin && (
                    <span className="radar-card-tipe-icon" style={{ color: TIPE_COLOR[mesin.tipe] }}>
                      <MesinTipeIcon tipe={mesin.tipe} size={12} />
                    </span>
                  )}
                  <span className="radar-card-tipe-label" style={{ color: mesin ? TIPE_COLOR[mesin.tipe] : "var(--text-faint)" }}>
                    {mesin?.tipe ?? "?"}
                  </span>
                </div>
                <div className="radar-card-corak">{corakLine}</div>
                <WaveProgressBar fraction={progress} trackColor="var(--bg-elevated-2)" fillColor={style.bar} height={3} animated />
              </div>
              <div className="radar-card-time" onClick={() => handleZoneClick(onEditWaktu)} role="button">
                {showDot && <span className={`ping-dot${remaining < 0 ? " danger" : ""}`} />}
                <div className="radar-card-time-text">
                  <div className="abs" style={{ color: style.text }}>
                    {absMinToTimeStr(est.estAbsMin)}
                  </div>
                  <div className="rel" style={{ color: remaining < 0 ? "var(--red-500)" : style.text }}>
                    {formatDeltaMin(remaining)}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="radar-card-face radar-card-back">
            {face === "ACTIONS" && (
              <div className="card-actions-face" onClick={() => setShowActionsFace(false)}>
                <CardFaceButton icon={<PauseIcon size={20} />} label="Jeda" accent="var(--amber-500)" onClick={onJeda} />
                <CardFaceButton icon={<DeleteIcon size={20} />} label="Hapus" accent="var(--red-500)" onClick={onHapus} />
              </div>
            )}
            {face === "PAUSED" && (
              <div className="card-paused-face">
                <div className="paused-label">Mc {est.mcNo} sedang dijeda</div>
                <CardFaceButton icon={<PlayIcon size={20} />} label="Lanjutkan" accent="var(--emerald-500)" onClick={onLanjutkan} />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function CardFaceButton({
  icon,
  label,
  accent,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  accent: string;
  onClick: () => void;
}) {
  return (
    <div className="card-face-btn">
      <button
        style={{ background: accent }}
        onClick={(e) => {
          e.stopPropagation();
          onClick();
        }}
        aria-label={label}
      >
        {icon}
      </button>
      <span style={{ color: accent }}>{label}</span>
    </div>
  );
}
