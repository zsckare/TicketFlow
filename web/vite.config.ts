import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],

    server: {
        port: 5174,
        strictPort: true,
        host: '0.0.0.0',

        allowedHosts: [
            'antonios-mac-mini.tail4c6258.ts.net',
        ],

        /*
         * El navegador llama a /api usando el mismo origen HTTPS.
         *
         * Tailscale:
         * https://antonios-mac-mini.tail4c6258.ts.net
         *              ↓
         * Vite :5174
         *              ↓
         * /api/* → API Gateway :8080/api/*
         *
         * No usamos rewrite porque el API Gateway ya espera
         * que las rutas comiencen con /api.
         */
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
})