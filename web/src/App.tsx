import { useEffect, useMemo, useState } from "react";
import { DoffStoreProvider, useDoffStore } from "./store/DoffStore";
import { UiStoreProvider, useUiStore } from "./store/UiStore";
import { ConsoleBar, type Mode } from "./components/ConsoleBar";
import { RadarScreen } from "./components/RadarScreen";
import { DoffingScreen } from "./components/DoffingScreen";
import { StatistikScreen } from "./components/StatistikScreen";
import { SettingsScreen } from "./components/SettingsScreen";
import { ToastHost } from "./components/ToastHost";
import { ConfirmDialog } from "./components/ConfirmDialog";
import { SettingsIcon } from "./components/Icons";
import { currentShiftStartAbsMin, formatDeltaMin, nowAbsMin } from "./domain/format";

type Screen = "main" | "statistik" | "settings";

function AppInner() {
  const { state, submitEstimasi, submitAktual, finishShift } = useDoffStore();
  const { showToast, showConfirm } = useUiStore();
  const [mode, setMode] = useState<Mode>("ESTIMASI");
  const [screen, setScreen] = useState<Screen>("main");
  const [input, setInput] = useState("");
  const [errorFlash, setErrorFlash] = useState(false);
  const [sendOk, setSendOk] = useState(false);
  const [staleDismissed, setStaleDismissed] = useState(false);
  const [, forceTick] = useState(0);

  // Tema: SYSTEM mengikuti preferensi OS, DARK/LIGHT dipaksa lewat atribut di <html>.
  useEffect(() => {
    const root = document.documentElement;
    function apply() {
      if (state.themeMode === "SYSTEM") {
        root.removeAttribute("data-theme");
      } else {
        root.setAttribute("data-theme", state.themeMode === "DARK" ? "dark" : "light");
      }
    }
    apply();
  }, [state.themeMode]);

  // Perbarui hitungan "shift tertinggal" & label waktu tiap 20 detik.
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

  const inputHint = useMemo(() => {
    if (mode !== "ESTIMASI") return null;
    const mcNo = input.trim().split(/\s+/)[0] ?? "";
    if (!/^\d{1,3}$/.test(mcNo)) return null;
    const tipe = state.db[mcNo]?.tipe;
    if (tipe === "TAPPET" || tipe === "CAM") return `Mc ${mcNo} → sisa menit, cth: ${mcNo} 45`;
    if (tipe === "D405") return `Mc ${mcNo} → yard berjalan, cth: ${mcNo} 280`;
    if (tipe === "D408") return `Mc ${mcNo} → jam counter, cth: ${mcNo} 12.30`;
    return null;
  }, [mode, input, state.db]);

  function flashError(msg: string) {
    setErrorFlash(true);
    setTimeout(() => setErrorFlash(false), 500);
    if (navigator.vibrate) navigator.vibrate(30);
    showToast(`⚠ ${msg}`);
  }

  function pulseOk() {
    setSendOk(true);
    setTimeout(() => setSendOk(false), 500);
  }

  function doSubmitEstimasi(cmd: string) {
    const result = submitEstimasi(cmd);
    if (result.ok) {
      pulseOk();
      showToast(result.msg);
      setInput("");
    } else {
      flashError(result.msg);
    }
  }

  function handleSend() {
    const cmd = input.trim().toUpperCase();
    if (!cmd) return;

    if (mode === "ESTIMASI") {
      const mcNo = cmd.split(/\s+/)[0];
      const existing = state.estimasi[mcNo];
      const remaining = existing ? existing.estAbsMin - nowAbsMin() : null;
      // Salah masuk mode ESTIMASI lalu ngetik cepat bisa nggak sadar menimpa timer
      // yang masih jalan — untuk estimasi yang masih aktif & mepet (<10 menit lagi),
      // minta konfirmasi dulu, bukan menimpa diam-diam.
      if (existing && remaining != null && remaining >= 0 && remaining < 10) {
        showConfirm(`Mc ${mcNo} sudah diestimasi ${formatDeltaMin(remaining)} lagi. Timpa dengan estimasi baru?`, () => {
          doSubmitEstimasi(cmd);
        });
      } else {
        doSubmitEstimasi(cmd);
      }
    } else {
      const result = submitAktual(cmd);
      if (result.ok) {
        pulseOk();
        if (navigator.vibrate) navigator.vibrate(15);
        showToast(result.msg, result.undo);
        setInput("");
      } else {
        flashError(result.msg);
      }
    }
  }

  function handleFinishShift() {
    if (state.aktual.length === 0 && Object.keys(state.estimasi).length === 0) {
      showToast("Tidak ada yang perlu diarsipkan");
      return;
    }
    showConfirm("Akhiri shift? Semua riwayat & estimasi berjalan akan diarsipkan ke Statistik.", () => {
      finishShift();
      showToast("Shift selesai ✓");
    });
  }

  return (
    <div className="app-shell">
      <ToastHost />

      <div className="app-header">
        <div className="app-title">
          Adoel<span className="dot">.</span>
        </div>
        <button className="icon-btn" onClick={() => setScreen("settings")} aria-label="Pengaturan">
          <SettingsIcon />
        </button>
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

      {mode === "ESTIMASI" ? <RadarScreen /> : <DoffingScreen onOpenStatistik={() => setScreen("statistik")} />}

      <ConsoleBar
        mode={mode}
        onModeChange={setMode}
        value={input}
        onChange={setInput}
        onSend={handleSend}
        hint={inputHint}
        errorFlash={errorFlash}
        sendOk={sendOk}
      />

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
