import { Ionicons } from "@expo/vector-icons";
import { Redirect, Tabs, router } from "expo-router";
import { Pressable, Text } from "react-native";
import { useAuth } from "../../src/lib/auth";
import { colors, fonts } from "../../src/theme";

function tabIcon(name: keyof typeof Ionicons.glyphMap) {
  return ({ color, size }: { color: string; size: number }) => (
    <Ionicons name={name} size={size} color={color} />
  );
}

export default function AppLayout() {
  const { session, ready, logout } = useAuth();
  if (!ready) return null;
  if (!session) return <Redirect href="/login" />;

  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: colors.ink },
        headerTintColor: colors.foam,
        headerTitleStyle: { fontFamily: fonts.display, fontSize: 20, fontWeight: "700" },
        headerShadowVisible: false,
        tabBarStyle: {
          backgroundColor: colors.ink,
          borderTopColor: "rgba(255,255,255,0.1)",
        },
        tabBarActiveTintColor: colors.amber,
        tabBarInactiveTintColor: "rgba(255,255,255,0.55)",
        tabBarLabelStyle: { fontFamily: fonts.bodyMedium, fontSize: 12 },
        headerRight: () => (
          <Pressable
            onPress={() => {
              void logout().then(() => router.replace("/"));
            }}
            style={{ paddingRight: 16 }}
          >
            <Text style={{ color: "rgba(255,255,255,0.5)", fontFamily: fonts.body, fontSize: 14 }}>Sair</Text>
          </Pressable>
        ),
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: "Saldo", tabBarIcon: tabIcon("wallet-outline"), tabBarLabel: "Saldo" }}
      />
      <Tabs.Screen
        name="transfer"
        options={{ title: "Transferir", tabBarIcon: tabIcon("swap-horizontal-outline"), tabBarLabel: "Transferir" }}
      />
      <Tabs.Screen
        name="transactions"
        options={{ title: "Extrato", tabBarIcon: tabIcon("receipt-outline"), tabBarLabel: "Extrato" }}
      />
    </Tabs>
  );
}
