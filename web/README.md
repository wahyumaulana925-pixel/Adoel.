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

Repo ini private, jadi GitHub Pages tidak bisa dipakai gratis. Deploy lewat
**Netlify** (mendukung repo private tanpa upgrade akun apa pun):

1. Daftar di https://app.netlify.com (bisa langsung "Sign up with GitHub")
2. **Add new site → Import an existing project → GitHub**, beri Netlify akses
   ke repo `Adoel.` (bisa dibatasi hanya repo ini)
3. Pengaturan build otomatis terbaca dari `netlify.toml` di root repo (base
   directory `web`, build command `npm run build`, publish directory `dist`)
   — tinggal klik **Deploy**
4. Setiap push ke `main` yang menyentuh `web/` otomatis ter-deploy ulang
