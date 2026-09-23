import type { ReactNode } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from "react-native";
import { colors, fonts } from "./theme";

export function Screen({ children, scroll = true }: { children: ReactNode; scroll?: boolean }) {
  const inner = scroll ? (
    <ScrollView contentContainerStyle={styles.pad} keyboardShouldPersistTaps="handled">
      {children}
    </ScrollView>
  ) : (
    <View style={[styles.pad, styles.fill, { justifyContent: "center" }]}>{children}</View>
  );

  return (
    <KeyboardAvoidingView style={styles.fill} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      {inner}
    </KeyboardAvoidingView>
  );
}

export function Brand({ size = 28 }: { size?: number }) {
  return <Text style={[styles.brand, { fontSize: size }]}>Financial Hub</Text>;
}

export function Display({ children, size = 36 }: { children: ReactNode; size?: number }) {
  return <Text style={[styles.display, { fontSize: size }]}>{children}</Text>;
}

export function Muted({ children }: { children: ReactNode }) {
  return <Text style={styles.muted}>{children}</Text>;
}

export function Field({
  label,
  error,
  ...input
}: { label: string; error?: string } & TextInputProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        placeholderTextColor="rgba(255,255,255,0.3)"
        style={[styles.input, error ? styles.inputError : null]}
        {...input}
      />
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

export function Alert({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <View style={styles.alert}>
      <Text style={styles.alertText}>{message}</Text>
    </View>
  );
}

export function PrimaryButton({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable style={[styles.primary, disabled && styles.disabled]} onPress={onPress} disabled={disabled}>
      <Text style={styles.primaryText}>{label}</Text>
    </Pressable>
  );
}

export function GhostButton({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable style={[styles.ghost, disabled && styles.disabled]} onPress={onPress} disabled={disabled}>
      <Text style={styles.ghostText}>{label}</Text>
    </Pressable>
  );
}

export function Card({ children }: { children: ReactNode }) {
  return <View style={styles.card}>{children}</View>;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ink },
  pad: { paddingHorizontal: 24, paddingVertical: 40, flexGrow: 1 },
  brand: { fontFamily: fonts.display, fontWeight: "700", color: colors.foam, letterSpacing: -0.4 },
  display: { fontFamily: fonts.display, fontWeight: "700", color: colors.foam, letterSpacing: -0.6 },
  muted: { fontFamily: fonts.body, color: colors.mutedSoft, fontSize: 15, lineHeight: 22, marginTop: 8 },
  field: { marginBottom: 18 },
  label: {
    fontFamily: fonts.bodyMedium,
    fontSize: 11,
    letterSpacing: 1.6,
    textTransform: "uppercase",
    color: "rgba(255,255,255,0.5)",
    marginBottom: 8,
  },
  input: {
    fontFamily: fonts.body,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: "rgba(255,255,255,0.05)",
    borderRadius: 8,
    color: colors.foam,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  inputError: {
    borderColor: "rgba(248,113,113,0.5)",
    backgroundColor: "rgba(239,68,68,0.05)",
  },
  fieldError: { fontFamily: fonts.body, color: "#fca5a5", fontSize: 12, marginTop: 6 },
  alert: {
    borderWidth: 1,
    borderColor: colors.dangerBorder,
    backgroundColor: colors.dangerBg,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginBottom: 16,
  },
  alertText: { fontFamily: fonts.body, color: "#fee2e2", fontSize: 14 },
  primary: {
    marginTop: 24,
    backgroundColor: colors.amber,
    borderRadius: 8,
    paddingVertical: 14,
    paddingHorizontal: 20,
    alignItems: "center",
  },
  primaryText: { fontFamily: fonts.bodySemi, color: colors.ink, fontSize: 14 },
  ghost: {
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.25)",
    borderRadius: 8,
    paddingVertical: 14,
    paddingHorizontal: 20,
    alignItems: "center",
    marginTop: 12,
  },
  ghostText: { fontFamily: fonts.body, color: "rgba(255,255,255,0.85)", fontSize: 14 },
  disabled: { opacity: 0.5 },
  card: {
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.1)",
    backgroundColor: "rgba(255,255,255,0.03)",
    borderRadius: 12,
    padding: 20,
  },
});
