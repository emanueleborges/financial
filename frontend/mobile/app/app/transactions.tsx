import { useCallback, useEffect, useState } from "react";
import { Text, View } from "react-native";
import { api, type StatementResponse } from "../../src/lib/api";
import { useAuth } from "../../src/lib/auth";
import { formatBRL } from "../../src/lib/money";
import { colors, fonts } from "../../src/theme";
import { Alert, Card, Display, GhostButton, Muted, Screen } from "../../src/ui";

export default function Transactions() {
  const { session } = useAuth();
  const [data, setData] = useState<StatementResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      setData(await api.listTransactions(session.document, session.token));
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no extrato");
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  async function reverse(id: string) {
    if (!session) return;
    try {
      await api.reverse(id, "Estorno solicitado pelo usuário", session.token);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha no estorno");
    }
  }

  return (
    <Screen>
      <Display>Extrato</Display>
      <Text style={{ fontFamily: fonts.display, color: colors.amber, fontSize: 22, marginTop: 8, marginBottom: 16 }}>
        Saldo {data ? formatBRL(data.currentBalance) : "—"}
      </Text>
      <Alert message={error} />
      {(data?.entries ?? []).map((item) => {
        const tx = item.transaction;
        const canReverse =
          tx.type === "TRANSFER" &&
          tx.status === "COMPLETED" &&
          tx.payerDocument === session?.document;
        return (
          <View key={tx.id} style={{ marginBottom: 12 }}>
            <Card>
              <Text style={{ fontFamily: fonts.bodySemi, color: colors.foam }}>
                {tx.type} · {formatBRL(tx.amount)}
              </Text>
              <Muted>
                {tx.payerDocument} → {tx.payeeDocument} · {tx.status}
              </Muted>
              {canReverse ? (
                <GhostButton label="Estornar" onPress={() => void reverse(tx.id)} />
              ) : null}
            </Card>
          </View>
        );
      })}
      <GhostButton label="Atualizar" onPress={() => void load()} />
    </Screen>
  );
}
