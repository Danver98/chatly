import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ command }) => {
  const shared = {
    plugins: [react(), tsconfigPaths(), tailwindcss()],
    build: {
      outDir: 'build', // Kept for CRA compatibility
    },
  };

  if (command === 'serve') {
    return {
      ...shared,
      server: {
        host: '0.0.0.0',      // Listen on all interfaces so Docker can expose it
        port: 3000,           // Keep your CRA-style port
        strictPort: true,     // Fail if 3000 is taken, don't silently pick another
        open: false,          // Don't try to open a browser inside the container
        watch: {
          usePolling: true,   // Reliable file watching on Windows/macOS hosts
        },
        hmr: {
          protocol: 'wss',    // Browser talks to Nginx over HTTPS, so HMR must be WSS
          host: 'localhost',  // The hostname your browser uses
          clientPort: 443,    // Nginx's HTTPS port — NOT the Vite container port
        },
      },
    };
  }

  // Production build (command === 'build')
  return shared;
});