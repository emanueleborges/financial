import { CameraView, useCameraPermissions } from "expo-camera";
import { useEffect, useRef, useState } from "react";
import { Image, StyleSheet, Text, View } from "react-native";
import { matchEnrolledFace } from "../lib/face";
import { colors, fonts } from "../theme";
import { GhostButton, PrimaryButton } from "../ui";

type EnrollProps = {
  mode?: "enroll";
  photoUri: string | null;
  onCaptured: (uri: string) => void;
  onClear: () => void;
};

type RecognizeProps = {
  mode: "recognize";
  document: string;
  busy?: boolean;
  onRecognized: () => void;
  onFailed: (message: string) => void;
};

type Props = EnrollProps | RecognizeProps;

export function FaceCapture(props: Props) {
  const cameraRef = useRef<CameraView>(null);
  const [permission, requestPermission] = useCameraPermissions();
  const [busy, setBusy] = useState(false);
  const [cameraReady, setCameraReady] = useState(false);
  const [status, setStatus] = useState("Enquadre o rosto. O modelo no aparelho vai comparar com a selfie cadastrada.");
  const scanning = useRef(false);

  const isRecognize = props.mode === "recognize";
  const recognizeDocument = isRecognize ? props.document : "";

  useEffect(() => {
    if (!isRecognize || !permission?.granted || !cameraReady || scanning.current) return;
    scanning.current = true;
    const timer = setTimeout(() => {
      void scanFace();
    }, 400);
    return () => {
      clearTimeout(timer);
    };
  }, [isRecognize, permission?.granted, cameraReady, recognizeDocument]);

  async function takePhoto() {
    if (!cameraRef.current || busy) return;
    setBusy(true);
    try {
      const photo = await cameraRef.current.takePictureAsync({ quality: 0.6, skipProcessing: true });
      if (photo?.uri && props.mode !== "recognize") props.onCaptured(photo.uri);
    } finally {
      setBusy(false);
    }
  }

  async function scanFace() {
    if (!isRecognize || !cameraRef.current) return;
    const document = props.document.replace(/\D/g, "");
    setBusy(true);
    setStatus("Modelo local comparando o rosto…");
    try {
      for (let attempt = 0; attempt < 4; attempt += 1) {
        const photo = await cameraRef.current.takePictureAsync({ quality: 0.55, skipProcessing: true });
        if (!photo?.uri) continue;
        const matched = await matchEnrolledFace(document, photo.uri);
        if (matched) {
          setStatus("Rosto reconhecido.");
          props.onRecognized();
          return;
        }
        setStatus(`Não reconhecido. Nova tentativa (${attempt + 1}/4)…`);
      }
      props.onFailed("Rosto não reconhecido. Enquadre o mesmo rosto cadastrado e tente de novo.");
    } catch (err) {
      props.onFailed(err instanceof Error ? err.message : "Falha ao reconhecer o rosto.");
    } finally {
      setBusy(false);
      scanning.current = false;
    }
  }

  if (!permission) {
    return <Text style={styles.hint}>Preparando câmera…</Text>;
  }

  if (!permission.granted) {
    return (
      <View style={styles.card}>
        <Text style={styles.title}>Reconhecimento facial</Text>
        <Text style={styles.hint}>Permita a câmera para reconhecer o rosto cadastrado.</Text>
        <PrimaryButton label="Permitir câmera" onPress={() => void requestPermission()} />
      </View>
    );
  }

  if (!isRecognize && props.photoUri) {
    return (
      <View style={styles.card}>
        <Text style={styles.title}>Reconhecimento facial</Text>
        <Text style={styles.hint}>Selfie cadastrada neste aparelho.</Text>
        <Image source={{ uri: props.photoUri }} style={styles.preview} />
        <GhostButton label="Tirar outra foto" onPress={props.onClear} />
      </View>
    );
  }

  return (
    <View style={styles.card}>
      <Text style={styles.title}>{isRecognize ? "Olhe para a câmera" : "Reconhecimento facial"}</Text>
      <Text style={styles.hint}>
        {isRecognize ? status : "Enquadre o rosto. A foto fica só neste aparelho."}
      </Text>
      <CameraView ref={cameraRef} style={styles.camera} facing="front" onCameraReady={() => setCameraReady(true)} />
      {isRecognize ? (
        <PrimaryButton
          label={busy || props.busy ? "Reconhecendo…" : "Tentar de novo"}
          onPress={() => {
            if (scanning.current || busy) return;
            scanning.current = true;
            void scanFace();
          }}
          disabled={busy || props.busy}
        />
      ) : (
        <PrimaryButton label={busy ? "Capturando…" : "Capturar rosto"} onPress={() => void takePhoto()} disabled={busy} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.1)",
    backgroundColor: "rgba(255,255,255,0.03)",
    borderRadius: 12,
    padding: 16,
    marginBottom: 8,
    marginTop: 20,
  },
  title: { fontFamily: fonts.bodySemi, color: colors.foam, fontSize: 16 },
  hint: { fontFamily: fonts.body, color: colors.mutedSoft, marginTop: 6, marginBottom: 10, fontSize: 14 },
  camera: { height: 240, borderRadius: 8, overflow: "hidden" },
  preview: { height: 240, borderRadius: 8 },
});
