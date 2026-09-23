import { router } from "expo-router";
import { useEffect, useState } from "react";
import { FaceCapture } from "../src/components/FaceCapture";
import { ApiClientError, api } from "../src/lib/api";
import { useAuth } from "../src/lib/auth";
import { biometricAvailable, biometricLabel } from "../src/lib/biometrics";
import { saveEnrolledFace } from "../src/lib/face";
import { parseBRLInput } from "../src/lib/money";
import { Alert, Brand, Display, Field, GhostButton, Muted, PrimaryButton, Screen } from "../src/ui";

export default function Register() {
  const { loginWithFace } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [document, setDocument] = useState("");
  const [password, setPassword] = useState("");
  const [balance, setBalance] = useState("1.000,00");
  const [faceUri, setFaceUri] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [canBio, setCanBio] = useState(false);
  const [bioLabel, setBioLabel] = useState("biometria");

  useEffect(() => {
    void Promise.all([biometricAvailable(), biometricLabel()]).then(([ok, label]) => {
      setCanBio(ok);
      setBioLabel(label);
    });
  }, []);

  async function onSubmit() {
    setError(null);
    const digits = document.replace(/\D/g, "");
    const initial = parseBRLInput(balance);
    if (name.trim().length < 2 || !email.includes("@") || (digits.length !== 11 && digits.length !== 14)) {
      setError("Preencha nome, e-mail e CPF/CNPJ válidos.");
      return;
    }
    if (password.length < 6 || initial == null || initial < 0) {
      setError("Senha (mín. 6) e saldo inicial válidos.");
      return;
    }
    if (!faceUri) {
      setError("Tire a foto do rosto para cadastrar o reconhecimento facial.");
      return;
    }
    setLoading(true);
    try {
      await api.createUser({
        name: name.trim(),
        email: email.trim(),
        document: digits,
        password,
        initialBalance: initial,
      });
      await saveEnrolledFace(digits, faceUri);
      await loginWithFace(digits, password);
      router.replace("/app");
    } catch (err) {
      if (err instanceof ApiClientError) {
        setError(Object.values(err.fields)[0] || err.message);
      } else {
        setError(err instanceof Error ? err.message : "Falha no cadastro");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <Brand size={22} />
      <Display>Criar conta</Display>
      <Muted>
        Cadastro com saldo inicial e selfie de reconhecimento facial.
        {canBio ? ` Depois o app pede ${bioLabel}.` : ""}
      </Muted>
      <FaceCapture photoUri={faceUri} onCaptured={setFaceUri} onClear={() => setFaceUri(null)} />
      <Field label="Nome" value={name} onChangeText={setName} />
      <Field label="E-mail" autoCapitalize="none" keyboardType="email-address" value={email} onChangeText={setEmail} />
      <Field
        label="CPF / CNPJ"
        keyboardType="number-pad"
        placeholder="52998224725"
        value={document}
        onChangeText={setDocument}
      />
      <Field label="Senha" secureTextEntry value={password} onChangeText={setPassword} />
      <Field label="Saldo inicial" value={balance} onChangeText={setBalance} placeholder="1.000,00" />
      <Alert message={error} />
      <PrimaryButton label={loading ? "Criando…" : "Criar conta"} onPress={() => void onSubmit()} disabled={loading} />
      <GhostButton label="Já tenho conta" onPress={() => router.push("/login")} />
    </Screen>
  );
}
