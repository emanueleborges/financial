export const colors = {
  ink: "#07131f",
  teal: "#145a55",
  foam: "#e8f1ef",
  amber: "#e8a838",
  amberHover: "#f0b84a",
  muted: "#8aa3a0",
  mutedSoft: "rgba(255,255,255,0.55)",
  card: "rgba(255,255,255,0.05)",
  border: "rgba(255,255,255,0.15)",
  danger: "#fecaca",
  dangerBg: "rgba(239,68,68,0.10)",
  dangerBorder: "rgba(248,113,113,0.30)",
};

import { Platform } from "react-native";

/** No Android, fonte não carregada some o texto. Só aplica Fraunces/Outfit no iOS. */
export const fonts = {
  display: Platform.OS === "ios" ? "Fraunces_700Bold" : undefined,
  body: Platform.OS === "ios" ? "Outfit_400Regular" : undefined,
  bodyMedium: Platform.OS === "ios" ? "Outfit_500Medium" : undefined,
  bodySemi: Platform.OS === "ios" ? "Outfit_600SemiBold" : undefined,
  bodyBold: Platform.OS === "ios" ? "Outfit_700Bold" : undefined,
};
