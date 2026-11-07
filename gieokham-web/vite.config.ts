import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "node:url";
export default defineConfig({
  plugins: [react()],
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  server: {
    allowedHosts: ["terminal.local"],
    port: 5173,
    strictPort: true,
    proxy: Object.fromEntries(
      ["/api", "/oauth2", "/login"].map((path) => [
        path,
        {
          target: process.env.API_TARGET || "http://localhost:8080",
          changeOrigin: true,
        },
      ]),
    ),
  },
});
