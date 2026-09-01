export type MesinTipe = "TAPPET" | "CAM" | "D405" | "D408";

export interface MesinData {
  tipe: MesinTipe;
  corak: string;
  targetYard: number | null;
  speed: number | null;
  koreksi: number | null;
  isActive?: boolean;
}

export function defaultMesinData(): MesinData {
  return { tipe: "TAPPET", corak: "-", targetYard: null, speed: null, koreksi: null };
}

export interface Estimasi {
  mcNo: string;
  estAbsMin: number;
  startAbsMin: number;
  corakOverride: string | null;
  yardOverride: number | null;
  pausedAtAbsMin: number | null;
}

export interface AktualEntry {
  id: number;
  mcNo: string;
  jam: string;
  ket: string;
  corakOverride: string | null;
  customYard: number | null;
  tsEpochMin: number | null;
}

export interface ShiftRecord {
  id: number;
  startedAtEpochMin: number;
  endedAtEpochMin: number;
  aktual: AktualEntry[];
  estimasiRemaining: Record<string, Estimasi>;
}

export type ThemeMode = "SYSTEM" | "LIGHT" | "DARK";

export const DEFAULT_KETERANGAN_SHORTCUTS: string[] = [];
export const DEFAULT_CORAK_SHORTCUTS: string[] = [];

export interface DoffState {
  db: Record<string, MesinData>;
  estimasi: Record<string, Estimasi>;
  aktual: AktualEntry[];
  nextId: number;
  themeMode: ThemeMode;
  history: ShiftRecord[];
  nextShiftId: number;
  onboardingSeen: boolean;
  keteranganShortcuts?: string[];
  corakShortcuts?: string[];
}

export type ProsesResult =
  | {
      ok: true;
      msg: string;
      mcNo: string;
      estAbs?: number;
      prevEst?: Estimasi | null;
      undo?: () => void;
      /** Entri aktual persis yang baru dibuat prosesBarisUmum (kalau ada) — dipakai redo
       * supaya mengembalikan baris yang sama (id sama), bukan menjalankan ulang command
       * dan membuat baris baru (yang akan menyisakan duplikat di Riwayat tiap siklus
       * undo/redo, karena nextId selalu maju). */
      entry?: AktualEntry;
    }
  | { ok: false; msg: string };
