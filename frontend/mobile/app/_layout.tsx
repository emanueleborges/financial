import { Fraunces_700Bold } from "@expo-google-fonts/fraunces";
import { Outfit_400Regular, Outfit_500Medium, Outfit_600SemiBold, Outfit_700Bold } from "@expo-google-fonts/outfit";
import { useFonts } from "expo-font";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { AuthProvider } from "../src/lib/auth";
import { colors, fonts } from "../src/theme";

export default function RootLayout() {
  useFonts({
    Fraunces_700Bold,
    Outfit_400Regular,
    Outfit_500Medium,
    Outfit_600SemiBold,
    Outfit_700Bold,
  });

  return (
    <AuthProvider>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: colors.ink },
          headerTintColor: colors.foam,
          headerTitleStyle: { fontFamily: fonts.display, fontSize: 20, fontWeight: "700" },
          headerShadowVisible: false,
          contentStyle: { backgroundColor: colors.ink },
        }}
      >
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="login" options={{ title: "Entrar", headerBackTitle: "Início" }} />
        <Stack.Screen name="register" options={{ title: "Criar conta", headerBackTitle: "Início" }} />
        <Stack.Screen name="app" options={{ headerShown: false }} />
      </Stack>
    </AuthProvider>
  );
}
