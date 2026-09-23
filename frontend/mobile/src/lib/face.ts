import * as FileSystem from "expo-file-system/legacy";
import { embedFace, embeddingsMatch } from "./faceEmbed";

function facePath(document: string) {
  const digits = document.replace(/\D/g, "");
  return `${FileSystem.documentDirectory}fh-face-${digits}.jpg`;
}

function embeddingPath(document: string) {
  const digits = document.replace(/\D/g, "");
  return `${FileSystem.documentDirectory}fh-face-${digits}.emb.json`;
}

export async function saveEnrolledFace(document: string, fromUri: string) {
  const dest = facePath(document);
  const info = await FileSystem.getInfoAsync(dest);
  if (info.exists) {
    await FileSystem.deleteAsync(dest, { idempotent: true });
  }
  await FileSystem.copyAsync({ from: fromUri, to: dest });
  const embedding = await embedFace(dest);
  await FileSystem.writeAsStringAsync(embeddingPath(document), JSON.stringify(embedding));
  return dest;
}

export async function hasEnrolledFace(document: string) {
  const info = await FileSystem.getInfoAsync(facePath(document));
  return info.exists;
}

async function loadOrBuildEmbedding(document: string) {
  const file = embeddingPath(document);
  const photo = facePath(document);
  const hashInfo = await FileSystem.getInfoAsync(file);
  if (hashInfo.exists) {
    const parsed = JSON.parse(await FileSystem.readAsStringAsync(file)) as unknown;
    if (Array.isArray(parsed) && parsed.every((item) => typeof item === "number")) return parsed;
  }
  const photoInfo = await FileSystem.getInfoAsync(photo);
  if (!photoInfo.exists) throw new Error("Não há selfie cadastrada neste aparelho.");
  const embedding = await embedFace(photo);
  await FileSystem.writeAsStringAsync(file, JSON.stringify(embedding));
  return embedding;
}

export async function matchEnrolledFace(document: string, probeUri: string) {
  const enrolled = await loadOrBuildEmbedding(document);
  const probe = await embedFace(probeUri);
  return embeddingsMatch(enrolled, probe);
}

export async function listEnrolledDocuments(): Promise<string[]> {
  const dir = FileSystem.documentDirectory;
  if (!dir) return [];
  const names = await FileSystem.readDirectoryAsync(dir);
  return names
    .filter((name) => name.startsWith("fh-face-") && name.endsWith(".jpg"))
    .map((name) => name.replace(/^fh-face-/, "").replace(/\.jpg$/, ""))
    .filter((digits) => digits.length === 11 || digits.length === 14);
}
