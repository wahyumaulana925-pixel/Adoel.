import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { prosesBarisKondisiMesin, prosesBarisUmum } from "../domain/commands";
import { buildDefaultDb } from "../domain/defaultDb";
import { nowAbsMin } from "../domain/format";
import { loadState, parseBackupJson, saveState, serializeState } from "../domain/storage";
import type { AktualEntry, DoffState, Estimasi, MesinData, ProsesResult, ThemeMode } from "../domain/types";

// Retensi riwayat: 30 HARI KALENDER (bukan jumlah shift) — sama seperti
// HISTORY_RETENTION_DAYS di DoffViewModel.kt.
const HISTORY_RETENTION_DAYS = 30;
const HISTORY_RETENTION_MIN = HISTORY_RETENTION_DAYS * 24 * 60;

interface DoffStore {
  state: DoffState;
  submitEstimasi: (line: string) => ProsesResult;
  submitAktual: (line: string) => ProsesResult;
  hapusEstimasi: (mcNo: string) => void;
  restoreEstimasi: (est: Estimasi) => void;
  hapusAktualById: (id: number) => void;
  restoreAktual: (entry: AktualEntry) => void;
  hapusShift: (id: number) => void;
  updateAktual: (id: number, ket: string, corakOverride: string | null, customYard: number | null) => void;
  finishShift: () => void;
  setMesin: (mcNo: string, data: MesinData) => void;
  resetMesin: (mcNo: string) => void;
  resetDb: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setOnboardingSeen: () => void;
  exportJson: () => string;
  importJson: (json: string) => DoffState | null;
}

const Ctx = createContext<DoffStore | null>(null);

export function DoffStoreProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<DoffState>(() => loadState());
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    saveState(state);
  }, [state]);

  const submitEstimasi = useCallback((line: string): ProsesResult => {
    let result!: ProsesResult;
    setState((s) => {
      const outcome = prosesBarisKondisiMesin(s, line, nowAbsMin());
      result = outcome.result;
      return outcome.newState;
    });
    return result;
  }, []);

  const hapusEstimasi = useCallback((mcNo: string) => {
    setState((s) => {
      const { [mcNo]: _removed, ...rest } = s.estimasi;
      return { ...s, estimasi: rest };
    });
  }, []);

  const restoreEstimasi = useCallback((est: Estimasi) => {
    setState((s) => ({ ...s, estimasi: { ...s.estimasi, [est.mcNo]: est } }));
  }, []);

  const hapusAktualById = useCallback((id: number) => {
    setState((s) => ({ ...s, aktual: s.aktual.filter((a) => a.id !== id) }));
  }, []);

  const restoreAktual = useCallback((entry: AktualEntry) => {
    setState((s) => (s.aktual.some((a) => a.id === entry.id) ? s : { ...s, aktual: [entry, ...s.aktual] }));
  }, []);

  // Dipakai baik oleh input konsol mode DOFFING maupun tombol "Doff" cepat di kartu
  // radar (mengetik "<mcNo>" saja setara dengan tap tombol doff) — identik dengan
  // handleDoff/handleCommand(Mode.AKTUAL) di MainScreen.kt Android. undo dibangun di
  // sini (bukan di commands.ts, yang murni) karena butuh akses ke aksi store lain
  // (hapusAktualById/restoreEstimasi), makanya didefinisikan setelah keduanya.
  const submitAktual = useCallback(
    (line: string): ProsesResult => {
      let result!: ProsesResult;
      setState((s) => {
        const outcome = prosesBarisUmum(s, line);
        result = outcome.result;
        if (result.ok && outcome.entryId !== undefined) {
          const entryId = outcome.entryId;
          const prevEst = result.prevEst;
          result = {
            ...result,
            undo: () => {
              hapusAktualById(entryId);
              if (prevEst) restoreEstimasi(prevEst);
            },
          };
        }
        return outcome.newState;
      });
      return result;
    },
    [hapusAktualById, restoreEstimasi],
  );

  const hapusShift = useCallback((id: number) => {
    setState((s) => ({ ...s, history: s.history.filter((h) => h.id !== id) }));
  }, []);

  const updateAktual = useCallback(
    (id: number, ket: string, corakOverride: string | null, customYard: number | null) => {
      setState((s) => ({
        ...s,
        aktual: s.aktual.map((a) => (a.id === id ? { ...a, ket, corakOverride, customYard } : a)),
      }));
    },
    [],
  );

  // "Selesai Shift": arsipkan aktual+estimasi berjalan ke history, lalu kosongkan
  // daftar aktif. Riwayat lebih tua dari 30 hari otomatis dibuang di sini.
  const finishShift = useCallback(() => {
    setState((s) => {
      if (s.aktual.length === 0 && Object.keys(s.estimasi).length === 0) return s;
      const now = nowAbsMin();
      const tsList = s.aktual.map((a) => a.tsEpochMin).filter((t): t is number => t !== null);
      const startList = Object.values(s.estimasi).map((e) => e.startAbsMin);
      const started = Math.min(...(tsList.length + startList.length > 0 ? [...tsList, ...startList] : [now]));
      const record = {
        id: s.nextShiftId,
        startedAtEpochMin: started,
        endedAtEpochMin: now,
        aktual: s.aktual,
        estimasiRemaining: s.estimasi,
      };
      const cutoff = now - HISTORY_RETENTION_MIN;
      return {
        ...s,
        estimasi: {},
        aktual: [],
        history: [record, ...s.history].filter((h) => h.endedAtEpochMin >= cutoff),
        nextShiftId: s.nextShiftId + 1,
      };
    });
  }, []);

  const setMesin = useCallback((mcNo: string, data: MesinData) => {
    setState((s) => ({ ...s, db: { ...s.db, [mcNo]: data } }));
  }, []);

  const resetMesin = useCallback((mcNo: string) => {
    setState((s) => ({ ...s, db: { ...s.db, [mcNo]: buildDefaultDb()[mcNo] } }));
  }, []);

  const resetDb = useCallback(() => {
    setState(() => ({
      db: buildDefaultDb(),
      estimasi: {},
      aktual: [],
      nextId: 1,
      themeMode: "SYSTEM",
      history: [],
      nextShiftId: 1,
      onboardingSeen: true,
    }));
  }, []);

  const setThemeMode = useCallback((mode: ThemeMode) => {
    setState((s) => ({ ...s, themeMode: mode }));
  }, []);

  const setOnboardingSeen = useCallback(() => {
    setState((s) => ({ ...s, onboardingSeen: true }));
  }, []);

  const exportJson = useCallback(() => serializeState(state), [state]);

  const importJson = useCallback((json: string): DoffState | null => {
    const parsed = parseBackupJson(json);
    if (parsed) setState(parsed);
    return parsed;
  }, []);

  const store: DoffStore = {
    state,
    submitEstimasi,
    submitAktual,
    hapusEstimasi,
    restoreEstimasi,
    hapusAktualById,
    restoreAktual,
    hapusShift,
    updateAktual,
    finishShift,
    setMesin,
    resetMesin,
    resetDb,
    setThemeMode,
    setOnboardingSeen,
    exportJson,
    importJson,
  };

  return <Ctx.Provider value={store}>{children}</Ctx.Provider>;
}

export function useDoffStore(): DoffStore {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useDoffStore must be used within DoffStoreProvider");
  return ctx;
}
