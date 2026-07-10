export type MesinTipe = "TAPPET" | "CAM" | "D405" | "D408";

export interface MesinData {
  tipe: MesinTipe;
  corak: string;
  targetYard: number | null;
  speed: number | null;
  koreksi: number | null;
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

export interface DoffState {
  db: Record<string, MesinData>;
  estimasi: Record<string, Estimasi>;
  aktual: AktualEntry[];
  nextId: number;
  themeMode: ThemeMode;
  history: ShiftRecord[];
  nextShiftId: number;
  onboardingSeen: boolean;
}

export type ProsesResult =
  | { ok: true; msg: string; mcNo: string; estAbs?: number; prevEst?: Estimasi | null; undo?: () => void }
  | { ok: false; msg: string };
