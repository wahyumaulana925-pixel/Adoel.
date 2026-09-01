import { describe, expect, it } from "vitest";
import { buildDefaultDb } from "./defaultDb";
import { prepareHandoverData, prepareMasterDbData, processScannedQr } from "./sync";
import type { DoffState } from "./types";

describe("sync domain module", () => {
  const baseState: DoffState = {
    db: buildDefaultDb(),
    estimasi: {
      "12": {
        mcNo: "12",
        estAbsMin: 800,
        startAbsMin: 700,
        corakOverride: null,
        yardOverride: null,
        pausedAtAbsMin: null,
      },
    },
    aktual: [
      {
        id: 1,
        mcNo: "10",
        jam: "14.30",
        ket: "14.30",
        corakOverride: "4500",
        customYard: 120,
        tsEpochMin: 870,
      },
    ],
    nextId: 2,
    themeMode: "DARK",
    history: [],
    nextShiftId: 1,
    onboardingSeen: true,
  };

  it("encodes and decodes handover QR data correctly", () => {
    // estAbsMin 800 > shiftEndAbs (start 700 + 480 = 1180, or nowAbs 200 -> shiftStart 0 -> shiftEnd 480)
    const qrData = prepareHandoverData(baseState, 200);
    expect(qrData).toContain('"type":"HANDOVER"');

    const receiverState: DoffState = {
      db: buildDefaultDb(),
      estimasi: {},
      aktual: [],
      nextId: 1,
      themeMode: "SYSTEM",
      history: [],
      nextShiftId: 1,
      onboardingSeen: true,
    };

    const result = processScannedQr(qrData, receiverState);
    expect(result).not.toBeNull();
    const merged = result!.state;
    expect(merged?.estimasi["12"]).toBeDefined();
    expect(merged?.estimasi["12"].estAbsMin).toBe(800);
    expect(merged?.db["12"]).toBeDefined();
  });

  it("encodes and decodes master DB QR data correctly", () => {
    const qrData = prepareMasterDbData(baseState);
    expect(qrData).toContain('"type":"MASTER_DB"');

    const receiverState: DoffState = {
      db: {},
      estimasi: {},
      aktual: [],
      nextId: 1,
      themeMode: "SYSTEM",
      history: [],
      nextShiftId: 1,
      onboardingSeen: true,
    };

    const result = processScannedQr(qrData, receiverState);
    expect(result).not.toBeNull();
    const merged = result!.state;
    expect(merged?.db["1"]).toBeDefined();
  });

  it("rejects invalid QR payload gracefully", () => {
    expect(processScannedQr("invalid string", baseState)).toBeNull();
    expect(processScannedQr(JSON.stringify({ type: "UNKNOWN", payload: "abc" }), baseState)).toBeNull();
  });
});
