import { useEffect, useMemo, useState, type MouseEvent as ReactMouseEvent } from "react";
import { DoffStoreProvider, useDoffStore } from "./store/DoffStore";
import { UiStoreProvider, useUiStore } from "./store/UiStore";
import { useConsoleHandlers } from "./hooks/useConsoleHandlers";
import { ConsoleBar } from "./components/ConsoleBar";
import { RadarScreen } from "./components/RadarScreen";
import { DoffingScreen } from "./components/DoffingScreen";
import { StatistikScreen } from "./components/StatistikScreen";
import { SettingsScreen } from "./components/SettingsScreen";
import { ToastHost } from "./components/ToastHost";
import { ConfirmDialog } from "./components/ConfirmDialog";
import { GuidedEstimasiSheet } from "./components/GuidedEstimasiSheet";
import { GuidedDoffingSheet } from "./components/GuidedDoffingSheet";
import { WaveProgressBar } from "./components/WaveProgressBar";
import { BarChartIcon, SettingsIcon } from "./components/Icons";
import { defaultMesinData } from "./domain/types";
import { currentShiftStartAbsMin, nowAbsMin } from "./domain/format";

type Page = "RADAR" | "RIWAYAT";
type Screen = "main" | "statistik" | "settings";

function AppInner() {
  const { state, setMesin, undo, redo, canUndo, canRedo } = useDoffStore();
  const { showToast } = useUiStore();
  const { handleEstimasiSubmit, handleAktualSubmit, handleFinishShift } = useConsoleHandlers();
  const [page, setPage] = useState<Page>("RADAR");
  const [screen, setScreen] = useState<Screen>("main");
  const [guidedEstimasiMcNo, setGuidedEstimasiMcNo] = useState<string | null>(null);
  const [guidedDoffingMcNo, setGuidedDoffingMcNo] = useState<string | null>(null);
  const [staleDismissed, setStaleDismissed] = useState(false);
  const [, forceTick] = useState(0);

  // Tema: SYSTEM mengikuti preferensi OS, DARK/LIGHT dipaksa lewat atribut di <html>.
  useEffect(() => {
    const root = document.documentElement;
    if (state.themeMode === "SYSTEM") {
      root.removeAttribute("data-theme");
    } else {
      root.setAttribute("data-theme", state.themeMode === "DARK" ? "dark" : "light");
    }
  }, [state.themeMode]);

  // Perbarui hitungan "shift tertinggal" & progress header tiap 20 detik.
  useEffect(() => {
    const id = setInterval(() => forceTick((n) => n + 1), 20000);
    return () => clearInterval(id);
  }, []);

  const nowAbs = nowAbsMin();
  const staleCount = useMemo(() => {
    const shiftStart = currentShiftStartAbsMin(nowAbs);
    return state.aktual.filter((a) => a.tsEpochMin != null && a.tsEpochMin < shiftStart).length;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.aktual, nowAbs]);

  // Un-snooze begitu batch yang tertinggal benar-benar sudah terselesaikan (mis.
  // lewat Selesai Shift), supaya shift lain yang lupa ditutup nanti tidak ikut
  // terdiam oleh "Nanti" yang ditekan untuk kejadian sebelumnya.
  useEffect(() => {
    if (staleCount === 0) setStaleDismissed(false);
  }, [staleCount]);

  const totalMc = Object.keys(state.db).length;
  const doffCount = state.aktual.length;

  function openGuidedEstimasi(mcNo: string) {
    if (!state.db[mcNo]) {
      showToast(`⚠ Mc ${mcNo} tidak ditemukan`);
      return;
    }
    setGuidedEstimasiMcNo(mcNo);
  }

  function openGuidedDoffing(mcNo: string) {
    if (!state.db[mcNo]) {
      showToast(`⚠ Mc ${mcNo} tidak ditemukan`);
      return;
    }
    setGuidedDoffingMcNo(mcNo);
  }

  function quickUpdateMesin(mcNo: string, corak: string, targetYard: number | null) {
    const mesin = state.db[mcNo] ?? defaultMesinData();
    setMesin(mcNo, { ...mesin, corak, targetYard });
  }

  // Tap di mana pun di luar field yang sedang aktif (kartu, tombol, area kosong)
  // keluar dari mode ketik & menutup keyboard — supaya field nomor mesin tidak
  // terus "siap ketik" tak berkesudahan sampai app dibuka lagi lain kali.
  function dismissKeyboardUnlessTypingHere(e: ReactMouseEvent) {
    const active = document.activeElement as HTMLElement | null;
    if (active && (active.tagName === "INPUT" || active.tagName === "TEXTAREA") && e.target !== active) {
      active.blur();
    }
  }

  return (
    <div className="app-shell" onClick={dismissKeyboardUnlessTypingHere}>
      <ToastHost />

      <div className="edge-fade edge-fade-top" />
      <div className="edge-fade edge-fade-bottom" />

      <div className="app-header floating-card">
        <div className="app-header-top">
          <div className="app-title">
            Adoel<span className="dot">.</span>
          </div>
          {totalMc > 0 && (
            <div className="shift-progress">
              <span>
                {doffCount}/{totalMc}
              </span>
              <WaveProgressBar
                fraction={totalMc > 0 ? doffCount / totalMc : 0}
                trackColor="var(--bg-elevated-2)"
                fillColor="var(--cyan-500)"
                height={4}
                width={70}
                animated={false}
              />
            </div>
          )}
          <button className="icon-btn" onClick={() => setScreen("statistik")} aria-label="Statistik">
            <BarChartIcon />
          </button>
          <button className="icon-btn" onClick={() => setScreen("settings")} aria-label="Pengaturan">
            <SettingsIcon />
          </button>
        </div>
        <div className="page-toggle">
          <button className={page === "RADAR" ? "active" : ""} onClick={() => setPage("RADAR")}>
            Radar
          </button>
          <button className={page === "RIWAYAT" ? "active" : ""} onClick={() => setPage("RIWAYAT")}>
            Riwayat
          </button>
        </div>
      </div>

      {staleCount > 0 && !staleDismissed && (
        <div style={{ padding: "0 12px 8px" }}>
          <div className="banner">
            <span className="msg">Ada {staleCount} catatan dari shift sebelumnya yang belum diarsipkan</span>
            <button onClick={handleFinishShift}>Selesai Shift</button>
            <button onClick={() => setStaleDismissed(true)}>Nanti</button>
          </div>
        </div>
      )}

      {page === "RADAR" ? (
        <RadarScreen onEditWaktu={openGuidedEstimasi} />
      ) : (
        <DoffingScreen onOpenStatistik={() => setScreen("statistik")} />
      )}

      <ConsoleBar
        onEstimasiClick={openGuidedEstimasi}
        onDoffingClick={openGuidedDoffing}
        onUndo={undo}
        onRedo={redo}
        canUndo={canUndo}
        canRedo={canRedo}
      />

      {guidedEstimasiMcNo && (
        <GuidedEstimasiSheet
          mcNo={guidedEstimasiMcNo}
          mesin={state.db[guidedEstimasiMcNo] ?? null}
          onDismiss={() => setGuidedEstimasiMcNo(null)}
          onSubmit={(value) => handleEstimasiSubmit(value, () => setGuidedEstimasiMcNo(null))}
          onQuickUpdate={(corak, targetYard) => quickUpdateMesin(guidedEstimasiMcNo, corak, targetYard)}
        />
      )}

      {guidedDoffingMcNo && (
        <GuidedDoffingSheet
          mcNo={guidedDoffingMcNo}
          mesin={state.db[guidedDoffingMcNo] ?? null}
          estimasi={state.estimasi[guidedDoffingMcNo] ?? null}
          onDismiss={() => setGuidedDoffingMcNo(null)}
          onSubmitDoffing={(value) => handleAktualSubmit(value, () => setGuidedDoffingMcNo(null))}
          onQuickUpdate={(corak, targetYard) => quickUpdateMesin(guidedDoffingMcNo, corak, targetYard)}
        />
      )}

      {screen === "statistik" && <StatistikScreen onClose={() => setScreen("main")} />}
      {screen === "settings" && <SettingsScreen onClose={() => setScreen("main")} />}

      <ConfirmDialog />
    </div>
  );
}

export default function App() {
  return (
    <DoffStoreProvider>
      <UiStoreProvider>
        <AppInner />
      </UiStoreProvider>
    </DoffStoreProvider>
  );
}
