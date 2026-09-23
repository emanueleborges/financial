# Financial Hub — Mobile (Expo)

Spec: [`specs/frontend/mobile.md`](../../specs/frontend/mobile.md)

```bash
cd frontend/mobile
npm install
npx expo start
```

API: o app resolve sozinho o host. No emulador Android usa `10.0.2.2:8080` (não `localhost`). No celular físico, o IP da máquina na LAN. Override: `EXPO_PUBLIC_API_URL`.

Face ID / digital é do aparelho. Nada biométrico vai para o ledger.
