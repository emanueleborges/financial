import { router } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { FaceCapture } from "../src/components/FaceCapture";
import { API_URL } from "../src/lib/api";
import { useAuth } from "../src/lib/auth";
import { biometricAvailable, biometricLabel } from "../src/lib/biometrics";
import { hasEnrolledFace, listEnrolledDocuments, saveEnrolledFace } from "../src/lib/face";
import { hasBiometricEnrollment, lastUsedDocument, listRememberedDocuments } from "../src/lib/session";
import { colors, fonts } from "../src/theme";
import { Alert, Brand, Display, Field, GhostButton, Muted, PrimaryButton, Screen } from "../src/ui";

function formatDocument(digits: string) {
  if (digits.length === 11) return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
  if (digits.length === 14) return digits.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, "$1.$2.$3/$4-$5");
  return digits;
}

export default function Login() {
  const { loginWithPassword, loginWithBiometric, loginWithFace } = useAuth();
  const [document, setDocument] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [bioReady, setBioReady] = useState(false);
  const [bioLabel, setBioLabel] = useState("biometria");
  const [faceMode, setFaceMode] = useState(false);
  const [faceUri, setFaceUri] = useState<string | null>(null);
  const [hasFace, setHasFace] = useState(false);
  const [accounts, setAccounts] = useState<string[]>([]);

  useEffect(() => {
    void Promise.all([
      hasBiometricEnrollment(),
      biometricAvailable(),
      biometricLabel(),
      listRememberedDocuments(),
      listEnrolledDocuments(),
      lastUsedDocument(),
    ]).then(([enrolled, available, label, remembered, faces, last]) => {
      setBioReady(enrolled && available);
      setBioLabel(label);
      const unique = [...new Set([...remembered, ...faces, ...(last ? [last] : [])])];
      setAccounts(unique);
      if (!document && unique[0]) setDocument(unique[0]);
    });
  }, []);

  useEffect(() => {
    const digits = document.replace(/\D/g, "");
    if (!digits) {
      setHasFace(false);
      setFaceMode(false);
      return;
    }
    void hasEnrolledFace(digits).then((ok) => {
      setHasFace(ok);
      if (ok) {
        setFaceMode(true);
        setFaceUri(null);
      }
    });
  }, [document]);

  async function onPassword() {
    setError(null);
    setLoading(true);
    try {
      await loginWithPassword(document, password, false);
      router.replace("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no login");
    } finally {
      setLoading(false);
    }
  }

  async function onBiometric() {
    setError(null);
    setLoading(true);
    try {
      await loginWithBiometric();
      router.replace("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha na biometria");
    } finally {
      setLoading(false);
    }
  }

  async function enterAfterFace() {
    const digits = document.replace(/\D/g, "");
    setLoading(true);
    setError(null);
    try {
      await loginWithFace(digits, password);
      router.replace("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no reconhecimento facial");
    } finally {
      setLoading(false);
    }
  }

  async function completeFaceEnroll(uri: string) {
    const digits = document.replace(/\D/g, "");
    if (!digits) {
      setError("Selecione o CPF/CNPJ da conta.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await saveEnrolledFace(digits, uri);
      setHasFace(true);
      await loginWithFace(digits, password);
      router.replace("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no reconhecimento facial");
    } finally {
      setLoading(false);
    }
  }

  async function onFace() {
    setError(null);
    const digits = document.replace(/\D/g, "");
    if (!digits) {
      setError("Selecione o CPF/CNPJ da conta.");
      return;
    }
    const already = await hasEnrolledFace(digits);
    setFaceMode(true);
    if (already) {
      setHasFace(true);
      return;
    }
    if (!faceUri) {
      setError("Ainda não há selfie neste aparelho. Tire a foto para cadastrar e entrar.");
      return;
    }
    await completeFaceEnroll(faceUri);
  }

  return (
    <Screen>
      <Brand size={22} />
      <Display>Entrar</Display>
      <Muted>Acesse com CPF/CNPJ e senha, {bioLabel} ou selfie cadastrada neste aparelho.</Muted>
      <Muted>API: {API_URL}</Muted>
      {accounts.length > 0 ? (
        <View style={styles.accounts}>
          <Text style={styles.accountsLabel}>Conta neste aparelho</Text>
          {accounts.map((item) => {
            const selected = document.replace(/\D/g, "") === item;
            return (
              <Pressable
                key={item}
                onPress={() => setDocument(item)}
                style={[styles.account, selected && styles.accountSelected]}
              >
                <Text style={[styles.accountText, selected && styles.accountTextSelected]}>
                  {formatDocument(item)}
                </Text>
              </Pressable>
            );
          })}
        </View>
      ) : null}
      <Field
        label={accounts.length > 0 ? "Outro CPF / CNPJ" : "CPF / CNPJ"}
        keyboardType="number-pad"
        value={document}
        onChangeText={setDocument}
        placeholder="52998224725"
      />
      <Field
        label="Senha"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
        placeholder="••••••••"
      />
      <Alert message={error} />
      <PrimaryButton label={loading ? "Entrando…" : "Entrar com senha"} onPress={() => void onPassword()} disabled={loading} />
      {bioReady ? (
        <GhostButton label={`Entrar com ${bioLabel}`} onPress={() => void onBiometric()} disabled={loading} />
      ) : null}
      {faceMode && hasFace ? (
        <FaceCapture
          key={document.replace(/\D/g, "")}
          mode="recognize"
          document={document}
          busy={loading}
          onRecognized={() => void enterAfterFace()}
          onFailed={setError}
        />
      ) : null}
      {faceMode && !hasFace ? (
        <FaceCapture
          photoUri={faceUri}
          onCaptured={(uri) => {
            setFaceUri(uri);
            void completeFaceEnroll(uri);
          }}
          onClear={() => setFaceUri(null)}
        />
      ) : null}
      {!hasFace ? (
        <GhostButton
          label={faceUri ? "Cadastrar selfie e entrar" : "Entrar com reconhecimento facial"}
          onPress={() => void onFace()}
          disabled={loading}
        />
      ) : null}
      <GhostButton label="Criar conta" onPress={() => router.push("/register")} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  accounts: { marginTop: 22, marginBottom: 8 },
  accountsLabel: {
    fontFamily: fonts.bodyMedium,
    fontSize: 11,
    letterSpacing: 1.6,
    textTransform: "uppercase",
    color: "rgba(255,255,255,0.5)",
    marginBottom: 8,
  },
  account: {
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.2)",
    backgroundColor: "rgba(255,255,255,0.04)",
    borderRadius: 8,
    paddingVertical: 14,
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  accountSelected: {
    borderColor: colors.amber,
    backgroundColor: "rgba(232,168,56,0.12)",
  },
  accountText: { fontFamily: fonts.bodySemi, color: colors.foam, fontSize: 16 },
  accountTextSelected: { color: colors.amber },
});
