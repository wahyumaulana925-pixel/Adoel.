# Adoel (Web/PWA)

Versi web (Progressive Web App) dari aplikasi Adoel — pencatat jadwal doffing
mesin Water Jet Loom. Port fungsional 1:1 dari aplikasi Android (`android/`):
rumus estimasi per tipe mesin, parsing perintah konsol, dan skema data sama
persis, supaya file backup JSON ("Cadangkan") dari aplikasi Android bisa
langsung diimpor di sini lewat Pengaturan → Pulihkan.

Data tersimpan sepenuhnya di perangkat (localStorage) — tidak ada backend/server.

## Menjalankan secara lokal

```sh
cd web
npm install
npm run dev
```

## Build produksi

```sh
npm run build
npm run preview
```

## Struktur

- `src/domain/` — logika inti (parsing perintah, rumus estimasi, format waktu,
  skema JSON) — port langsung dari `android/app/.../data/*.kt` dan
  `viewmodel/DoffViewModel.kt`. Ubah di sini kalau aturan bisnis berubah, dan
  pertahankan agar tetap identik dengan versi Android.
- `src/store/` — state aplikasi (React Context) + persistensi localStorage.
- `src/components/` — layar & komponen UI (Radar/Estimasi, Doffing, Statistik,
  Pengaturan).

## Deploy

`.github/workflows/deploy-web.yml` otomatis build & deploy ke GitHub Pages
setiap push ke `main` yang menyentuh folder `web/`.
