// Port 1:1 dari fungsi parsing di data/Models.kt.

/** Format "H.MM" / "H,MM" (mis. "14.30") atau "HHMM" (mis. "1430"). Dipakai untuk
 * bacaan counter D408. */
export function parseJam(str: string): number | null {
  const s = str.trim().replace(",", ".");
  const m1 = /^(\d{1,2})[.:](\d{2})$/.exec(s);
  if (m1) {
    const h = parseInt(m1[1], 10);
    const mi = parseInt(m1[2], 10);
    if (h >= 0 && h <= 23 && mi >= 0 && mi <= 59) return h * 60 + mi;
    return null;
  }
  const m2 = /^(\d{2})(\d{2})$/.exec(s);
  if (m2) {
    const h = parseInt(m2[1], 10);
    const mi = parseInt(m2[2], 10);
    if (h >= 0 && h <= 23 && mi >= 0 && mi <= 59) return h * 60 + mi;
    return null;
  }
  return null;
}

/** Format "H.MM" (jam+menit, mis. "1.30" = 90 menit), "NNNm"/"NNNmenit" (mis. "90m"),
 * atau angka polos (mis. "90" = 90 menit). Dipakai untuk durasi TAPPET/CAM. */
export function parseDurasi(str: string): number | null {
  const s = str.trim().replace(",", ".");
  const m1 = /^(\d{1,2})\.(\d{2})$/.exec(s);
  if (m1) {
    const h = parseInt(m1[1], 10);
    const mi = parseInt(m1[2], 10);
    if (mi >= 0 && mi <= 59) return h * 60 + mi;
    return null;
  }
  const m2 = /^(\d{1,4})m(?:enit)?$/i.exec(s);
  if (m2) return parseInt(m2[1], 10);
  const m3 = /^(\d{1,4})$/.exec(s);
  if (m3) return parseInt(m3[1], 10);
  return null;
}

/** Normalisasi kode keterangan singkat (case-insensitive, variasi spasi/titik) ke
 * bentuk baku yang ditampilkan. */
export function standarisasiKeterangan(raw: string): string {
  const t = raw.trim().toLowerCase();
  if (t === "hb") return "HB";
  if (["lp", "p.lp", "p. lp", "p lp"].includes(t)) return "P.LP";
  if (["sn", "p.sn", "p. sn", "p sn", "snarling"].includes(t)) return "P.SN";
  if (["oh", "p.oh", "p. oh", "p oh", "overhaul"].includes(t)) return "P.OH";
  if (["el", "p.el", "p. el", "p el", "elektrik"].includes(t)) return "P.EL";
  if (["sel", "selvedge", "p.sel", "p. sel", "p sel"].includes(t)) return "P.Sel";
  return raw.trim();
}
