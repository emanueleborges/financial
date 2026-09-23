import { useCallback, useEffect, useState } from "react";
import { Text } from "react-native";
import { api, type BalanceResponse, type UserResponse } from "../../src/lib/api";
import { useAuth } from "../../src/lib/auth";
import { formatBRL } from "../../src/lib/money";
import { colors, fonts } from "../../src/theme";
import { Alert, Card, Display, GhostButton, Muted, Screen } from "../../src/ui";

export default function Home() {
  const { session } = useAuth();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [balance, setBalance] = useState<BalanceResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      const [u, b] = await Promise.all([
        api.getUser(session.document, session.token),
        api.getBalance(session.document, session.token),
      ]);
      setUser(u);
      setBalance(b);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar");
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Screen>
      <Text style={{ fontFamily: fonts.body, fontSize: 11, letterSpacing: 1.6, textTransform: "uppercase", color: "rgba(255,255,255,0.45)" }}>
        Sua conta
      </Text>
      <Display size={36}>{user?.name ?? "…"}</Display>
      <Card>
        <Text style={{ fontFamily: fonts.body, fontSize: 10, letterSpacing: 1.4, textTransform: "uppercase", color: "rgba(255,255,255,0.45)" }}>
          Saldo disponível
        </Text>
        <Text style={{ fontFamily: fonts.display, color: colors.amber, fontSize: 28, marginTop: 6 }}>
          {balance ? formatBRL(balance.balance) : "—"}
        </Text>
        <Muted>CPF / CNPJ {user?.document ?? session?.document}</Muted>
        <Muted>{user?.email}</Muted>
        <Muted>Limite diário {user ? formatBRL(user.dailyLimit) : "—"}</Muted>
      </Card>
      <Alert message={error} />
      <GhostButton label="Atualizar" onPress={() => void load()} />
    </Screen>
  );
}
