import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// Nama repo GitHub punya titik di akhir ("Adoel.") — base path GitHub Pages untuk
// project site harus persis "/Adoel./" (root repo, bukan user/organization site).
export default defineConfig({
  base: "/Adoel./",
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.svg", "icons/icon-192.png", "icons/icon-512.png"],
      manifest: {
        name: "Adoel — Jadwal Doffing",
        short_name: "Adoel",
        description: "Pencatat jadwal doffing mesin Water Jet Loom",
        theme_color: "#09090b",
        background_color: "#09090b",
        display: "standalone",
        start_url: "/Adoel./",
        scope: "/Adoel./",
        icons: [
          { src: "icons/icon-192.png", sizes: "192x192", type: "image/png" },
          { src: "icons/icon-512.png", sizes: "512x512", type: "image/png" },
          { src: "icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
        ],
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,svg,png,ico}"],
      },
    }),
  ],
});
