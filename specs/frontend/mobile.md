# Spec Frontend Mobile — Financial Hub

**Status:** active  
**Stack:** React Native (Expo + expo-router)  
**Código:** `frontend/mobile/`  
**API:** `specs/backend/api/rest-v1.md`  
**Telas:** mesmo contrato de [`overview.md`](overview.md)

## Objetivo

App nativo equivalente às UIs web (cadastro, login, saldo, transferência, extrato), com **login biométrico** (Face ID / impressão digital) cadastrado **na criação da conta**.

## Identidade e biometria

- CPF/CNPJ continua a chave pública (igual à web).
- A biometria do **sensor** (Face ID, Touch ID, digital Android) e a **selfie de matrícula** ficam no dispositivo.
- **Proibido** enviar foto, template ou hash biométrico à API, PostgreSQL, Oracle ou Mongo.
- No cadastro (`/register`) o usuário **tira uma foto do rosto** (câmera frontal) antes de criar a conta. Sem selfie, o cadastro não segue.
- Depois do `POST /users` + login, o app pede Face ID / digital. Se confirmar, o `refreshToken` fica no SecureStore e `biometricEnabled=true`.
- A selfie é gravada só no armazenamento local do app (`fh-face-{documento}.jpg`). O reconhecimento **não** usa API/nuvem: um **modelo on-device** (detecção de face + embedding LBP/HOG/Gabor + similaridade de cosseno) gera um vetor em `fh-face-{documento}.emb.json`.
- Login com selfie: ao selecionar o CPF já matriculado, a **câmera frontal abre**, o modelo compara o rosto ao embedding da matrícula e só autentica se a similaridade passar o limiar. A foto de login **não** substitui a selfie de matrícula.
- Se ainda não houver selfie neste aparelho, o fluxo cadastra a foto e entra.
- CPF/CNPJ já usado neste aparelho (sessão anterior ou selfie local) aparece como **opção selecionável** no login. O campo livre continua para outra conta.
- Sair da conta remove só o access token; o refresh fica no aparelho para o próximo reconhecimento facial.
- Abas autenticadas têm ícones: Saldo, Transferir, Extrato.
- Sem câmera: o cadastro informa o erro; a conta web com senha continua válida.

## Visual

Mesmo sistema das UIs web (`overview.md`): ink `#07131f`, teal `#145a55`, foam `#e8f1ef`, âmbar `#e8a838`, Fraunces (títulos) e Outfit (corpo).

## Rotas

| Path | Tela | Auth |
|------|------|------|
| `/` | Landing | público |
| `/login` | Login senha + biometria | público |
| `/register` | Cadastro + matrícula biométrica | público |
| `/app` | Saldo | JWT |
| `/app/transfer` | Transferência (senha obrigatória) | JWT |
| `/app/transactions` | Extrato + estorno; cada lançamento mostra data e hora locais (`createdAt`) | JWT |

## Ambiente

`EXPO_PUBLIC_API_URL` sobrescreve o default.

No **smartphone** (Expo Go), o app usa o IP LAN do Metro (`http://<ip-do-mac>:8080`). Mac e celular no **mesmo Wi‑Fi**. Não use `localhost` nem `10.0.2.2` no aparelho físico.

```bash
EXPO_PUBLIC_API_URL=http://192.168.x.x:8080 npx expo start --lan
```

Emulador Android (Pixel AVD): API em `http://10.0.2.2:8080`. iOS Simulator: `http://localhost:8080`. No Android as fontes web não são aplicadas até estarem no SO (texto some se `fontFamily` inválida).

## Build local

```bash
cd frontend/mobile
npm install
npx expo start
```

## Critérios de aceite

1. Fluxo web equivalente: cadastro → login (CPF) → saldo → transferir (com senha) → extrato → estornar.
2. Cadastro de conta nova exige selfie de reconhecimento facial e oferece Face ID / digital no mesmo fluxo.
3. Login biométrico não envia senha nem biometria à API (só `refreshToken`).
4. JWT no SecureStore; rotas `/app` protegidas.
5. Sem UUID de usuário na UI.
6. Cada item do extrato exibe data e hora da movimentação no fuso do aparelho (`dd/MM/aaaa HH:mm:ss`).
