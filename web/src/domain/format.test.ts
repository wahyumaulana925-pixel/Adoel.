import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  absMinToTimeStr,
  currentShiftStartAbsMin,
  formatDeltaMin,
  formatYard,
  jamKeShiftAbs,
  shiftNumberForEpochMin,
} from "./format";

// Port dari ShiftMathTest.kt + bagian format ModelsTest.kt. Fungsi web memakai zona LOKAL
// perangkat; helper epochMin di bawah juga membangun tanggal di zona lokal, jadi keduanya
// saling meniadakan efek zona — assertion tetap benar di runner CI zona apa pun tanpa pin TZ.
function epochMin(y: number, mo: number, d: number, h: number, mi: number): number {
  return Math.floor(new Date(y, mo - 1, d, h, mi, 0, 0).getTime() / 60000);
}

describe("formatDeltaMin", () => {
  it("sisa waktu → Nm / NjMm", () => {
    expect(formatDeltaMin(0)).toBe("0m");
    expect(formatDeltaMin(5)).toBe("5m");
    expect(formatDeltaMin(60)).toBe("1j0m");
    expect(formatDeltaMin(85)).toBe("1j25m");
    expect(formatDeltaMin(125)).toBe("2j5m");
  });

  it("overdue diberi tanda minus U+2212", () => {
    expect(formatDeltaMin(-5)).toBe("−5m");
    expect(formatDeltaMin(-85)).toBe("−1j25m");
  });
});

describe("formatYard", () => {
  it("bulat tanpa .0, pecahan dipertahankan", () => {
    expect(formatYard(303)).toBe("303");
    expect(formatYard(165)).toBe("165");
    expect(formatYard(1.25)).toBe("1.25");
    expect(formatYard(0.158)).toBe("0.158");
  });
});

describe("shiftNumberForEpochMin", () => {
  it("Shift 1 06-14, Shift 2 14-22, Shift 3 22-06", () => {
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 15, 6, 0))).toBe(1);
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 15, 13, 59))).toBe(1);
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 15, 14, 0))).toBe(2);
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 15, 21, 59))).toBe(2);
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 15, 22, 0))).toBe(3);
    expect(shiftNumberForEpochMin(epochMin(2026, 1, 16, 5, 59))).toBe(3);
  });
});

describe("currentShiftStartAbsMin", () => {
  it("batas 06/14/22 terdekat sebelum/pas epochMin", () => {
    expect(currentShiftStartAbsMin(epochMin(2026, 1, 15, 10, 0))).toBe(epochMin(2026, 1, 15, 6, 0));
    expect(currentShiftStartAbsMin(epochMin(2026, 1, 15, 14, 0))).toBe(epochMin(2026, 1, 15, 14, 0));
    expect(currentShiftStartAbsMin(epochMin(2026, 1, 15, 23, 10))).toBe(epochMin(2026, 1, 15, 22, 0));
  });

  it("dini hari termasuk Shift 3 yang mulai 22.00 kemarin", () => {
    expect(currentShiftStartAbsMin(epochMin(2026, 1, 16, 2, 0))).toBe(epochMin(2026, 1, 15, 22, 0));
  });
});

describe("absMinToTimeStr", () => {
  it("abs-minute → HH.mm lokal", () => {
    expect(absMinToTimeStr(epochMin(2026, 1, 15, 16, 20))).toBe("16.20");
    expect(absMinToTimeStr(epochMin(2026, 1, 15, 7, 5))).toBe("07.05");
  });
});

describe("jamKeShiftAbs", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("hari yang sama saat dalam 12 jam", () => {
    vi.setSystemTime(new Date(2026, 0, 15, 12, 0));
    expect(jamKeShiftAbs(14 * 60)).toBe(epochMin(2026, 1, 15, 14, 0));
  });

  it("wrap ke besok saat lintas tengah malam", () => {
    vi.setSystemTime(new Date(2026, 0, 15, 23, 30));
    expect(jamKeShiftAbs(15)).toBe(epochMin(2026, 1, 16, 0, 15));
  });

  it("wrap ke kemarin saat lintas tengah malam", () => {
    vi.setSystemTime(new Date(2026, 0, 16, 0, 30));
    expect(jamKeShiftAbs(23 * 60 + 45)).toBe(epochMin(2026, 1, 15, 23, 45));
  });

  it("tepat +12 jam tetap hari yang sama (batas > 720)", () => {
    vi.setSystemTime(new Date(2026, 0, 15, 0, 0));
    expect(jamKeShiftAbs(12 * 60)).toBe(epochMin(2026, 1, 15, 12, 0));
  });
});
