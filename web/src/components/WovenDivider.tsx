import { useElementWidth } from "../hooks/useElementWidth";

/** Pengganti garis pembatas polos — garis putus-putus mengikuti kurva sinus landai, bukan
 * garis lurus datar, sebagai isyarat "benang" tanpa jadi foto literal. Satu warna dan
 * amplitudo dangkal sengaja — versi sebelumnya menggambar ini dua kali (dua pola dash
 * beda fase, satu diberi warna cyan) supaya terbaca sebagai benang bersilang, tapi itu
 * jadi lebih ramai/mencolok dari yang seharusnya untuk sekadar pembatas. Port 1:1 dari
 * WovenDivider di Texture.kt. */
export function WovenDivider() {
  const { ref, width } = useElementWidth<HTMLDivElement>();
  const height = 16;
  const midY = height / 2;
  const amplitude = 1.5;
  const wavelength = 16;
  const step = 1;

  let d = "";
  if (width > 0) {
    const pts: string[] = [];
    for (let x = 0; x <= width; x += step) {
      const y = midY + amplitude * Math.sin((2 * Math.PI * x) / wavelength);
      pts.push(`${x.toFixed(1)},${y.toFixed(1)}`);
    }
    d = "M" + pts.join(" L");
  }

  return (
    <div ref={ref} className="woven-divider">
      {width > 0 && (
        <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
          <path d={d} fill="none" stroke="var(--border)" strokeWidth={1} strokeDasharray="10 6" />
        </svg>
      )}
    </div>
  );
}
