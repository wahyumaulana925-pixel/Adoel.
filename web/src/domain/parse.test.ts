import { describe, it, expect } from "vitest";
import { parseDurasi, parseJam, standarisasiKeterangan } from "./parse";

// Port dari ModelsTest.kt (bagian parsing) — logika yang paling rawan regresi diam-diam.

describe("parseDurasi", () => {
  it("format jam.menit", () => {
    expect(parseDurasi("1.25")).toBe(85); // 1 jam 25 menit
    expect(parseDurasi("1.05")).toBe(65);
    expect(parseDurasi("0.00")).toBe(0);
  });

  it("menit polos", () => {
    expect(parseDurasi("45")).toBe(45);
    expect(parseDurasi("90")).toBe(90);
    expect(parseDurasi("1234")).toBe(1234);
  });

  it("akhiran m / menit (case-insensitive)", () => {
    expect(parseDurasi("30m")).toBe(30);
    expect(parseDurasi("30menit")).toBe(30);
    expect(parseDurasi("30MENIT")).toBe(30);
  });

  it("koma dianggap titik desimal", () => {
    expect(parseDurasi("1,25")).toBe(85);
  });

  it("input tidak valid → null", () => {
    expect(parseDurasi("1.60")).toBeNull(); // menit 60 di luar 0..59
    expect(parseDurasi("abc")).toBeNull();
    expect(parseDurasi("")).toBeNull();
  });
});

describe("parseJam", () => {
  it("dengan pemisah . atau :", () => {
    expect(parseJam("07.30")).toBe(450);
    expect(parseJam("7:30")).toBe(450);
    expect(parseJam("23.59")).toBe(1439);
    expect(parseJam("00.00")).toBe(0);
  });

  it("empat digit HHMM", () => {
    expect(parseJam("0730")).toBe(450);
    expect(parseJam("2359")).toBe(1439);
  });

  it("input tidak valid → null", () => {
    expect(parseJam("24.00")).toBeNull(); // jam 24 tidak valid
    expect(parseJam("07.60")).toBeNull(); // menit 60 tidak valid
    expect(parseJam("730")).toBeNull(); // 3 digit, bukan HHMM
    expect(parseJam("abc")).toBeNull();
  });
});

describe("standarisasiKeterangan", () => {
  it("alias singkat → bentuk baku", () => {
    expect(standarisasiKeterangan("hb")).toBe("HB");
    expect(standarisasiKeterangan("HB")).toBe("HB");
    expect(standarisasiKeterangan("lp")).toBe("P.LP");
    expect(standarisasiKeterangan("p.sn")).toBe("P.SN");
    expect(standarisasiKeterangan("snarling")).toBe("P.SN");
    expect(standarisasiKeterangan("overhaul")).toBe("P.OH");
    expect(standarisasiKeterangan("elektrik")).toBe("P.EL");
    expect(standarisasiKeterangan("sel")).toBe("P.Sel");
  });

  it("paritas Android: matching/match → MATCHING", () => {
    expect(standarisasiKeterangan("matching")).toBe("MATCHING");
    expect(standarisasiKeterangan("match")).toBe("MATCHING");
    expect(standarisasiKeterangan("MATCHING")).toBe("MATCHING");
  });

  it("teks bebas diteruskan apa adanya (di-trim)", () => {
    expect(standarisasiKeterangan("teks bebas")).toBe("teks bebas");
    expect(standarisasiKeterangan("  catatan  ")).toBe("catatan");
  });
});
