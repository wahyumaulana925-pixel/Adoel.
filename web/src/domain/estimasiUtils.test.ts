import { describe, it, expect } from "vitest";
import {
  effectiveRemaining,
  nearestUpcoming,
  partitionSegeraMenunggu,
  selisihKoreksiD408,
  sortedByNearest,
  urgencyLevel,
} from "./estimasiUtils";
import type { Estimasi } from "./types";

// Port dari EstimasiUtilsTest.kt — logika radar (urutan/urgensi/jeda) diuji numerik.

function est(mcNo: string, estAbsMin: number, pausedAtAbsMin: number | null = null): Estimasi {
  return { mcNo, estAbsMin, startAbsMin: 0, corakOverride: null, yardOverride: null, pausedAtAbsMin };
}

describe("urgencyLevel", () => {
  it("batas 30 / 10 / 0 menit", () => {
    expect(urgencyLevel(31)).toBe("CALM");
    expect(urgencyLevel(30)).toBe("SOON");
    expect(urgencyLevel(11)).toBe("SOON");
    expect(urgencyLevel(10)).toBe("IMMINENT");
    expect(urgencyLevel(1)).toBe("IMMINENT");
    expect(urgencyLevel(0)).toBe("OVERDUE");
    expect(urgencyLevel(-45)).toBe("OVERDUE");
  });
});

describe("sortedByNearest", () => {
  it("urut naik berdasarkan estAbsMin", () => {
    const sorted = sortedByNearest({ a: est("a", 300), b: est("b", 100), c: est("c", 200) });
    expect(sorted.map((e) => e.mcNo)).toEqual(["b", "c", "a"]);
  });
});

describe("partitionSegeraMenunggu", () => {
  it("memisah yang jatuh tempo dari yang menunggu", () => {
    const sorted = [est("late", 90), est("due", 100), est("soon", 110)];
    const [segera, menunggu] = partitionSegeraMenunggu(sorted, 100);
    expect(segera.map((e) => e.mcNo)).toEqual(["late", "due"]);
    expect(menunggu.map((e) => e.mcNo)).toEqual(["soon"]);
  });
});

describe("nearestUpcoming", () => {
  it("prioritas overdue paling awal, lalu yang paling dekat", () => {
    expect(nearestUpcoming({ late: est("late", 90), soon: est("soon", 110) }, 100)?.mcNo).toBe("late");
    expect(nearestUpcoming({ soon: est("soon", 110) }, 100)?.mcNo).toBe("soon");
    expect(nearestUpcoming({}, 100)).toBeNull();
  });
});

describe("effectiveRemaining", () => {
  it("menghitung mundur dari now saat berjalan", () => {
    expect(effectiveRemaining(est("a", 150), 100)).toBe(50);
  });

  it("beku di titik jeda saat dijeda (now diabaikan)", () => {
    expect(effectiveRemaining(est("a", 150, 120), 100)).toBe(30);
    expect(effectiveRemaining(est("a", 150, 120), 99999)).toBe(30);
  });
});

describe("selisihKoreksiD408", () => {
  it("menghitung selisih counter dalam menit", () => {
    expect(selisihKoreksiD408(12 * 60 + 48, 12 * 60 + 30)).toBe(18);
  });

  it("memilih selisih terdekat saat melewati tengah malam", () => {
    expect(selisihKoreksiD408(5, 23 * 60 + 50)).toBe(15);
  });
});
