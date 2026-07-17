import { useElementWidth } from "../hooks/useElementWidth";

/** Pengganti garis pembatas polos — dua pola dash pada satu garis sinus yang sama,
 * digeser fasenya persis sepanjang satu dash supaya yang kedua mengisi celah yang
 * pertama, dengan warna sedikit berbeda. Terbaca sebagai dua benang bersilang (lungsin/
 * pakan) alih-alih garis putus-putus datar. Port 1:1 dari WovenDivider di Texture.kt. */
export function WovenDivider() {
  const { ref, width } = useElementWidth<HTMLDivElement>();
  const height = 16;
  const midY = height / 2;
  const amplitude = 3;
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
          <path d={d} fill="none" stroke="var(--border)" strokeWidth={1.5} strokeDasharray="10 6" strokeDashoffset={0} />
          <path
            d={d}
            fill="none"
            stroke="color-mix(in srgb, var(--border) 55%, var(--cyan-500))"
            strokeWidth={1.5}
            strokeDasharray="10 6"
            strokeDashoffset={-10}
          />
        </svg>
      )}
    </div>
  );
}
