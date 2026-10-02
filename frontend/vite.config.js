import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server forwards every /api request to the Java backend, so the browser sees a single
// origin and the frontend code can simply call fetch('/api/...').
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
