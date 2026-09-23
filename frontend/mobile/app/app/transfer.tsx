import { useState } from "react";
import { api } from "../../src/lib/api";
import { useAuth } from "../../src/lib/auth";
import { formatBRL, parseBRLInput } from "../../src/lib/money";
import { Alert, Display, Field, Muted, PrimaryButton, Screen } from "../../src/ui";

export default function Transfer() {
  const { session } = useAuth();
  const [payee, setPayee] = useState("");
  const [amount, setAmount] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit() {
    if (!session) return;
    const digits = payee.replace(/\D/g, "");
    const value = parseBRLInput(amount);
    if ((digits.length !== 11 && digits.length !== 14) || value == null || value <= 0) {
      setMessage("Informe CPF/CNPJ e valor válidos.");
      return;
    }
    if (password.length < 6) {
      setMessage("Informe a senha da conta para confirmar.");
      return;
    }
    setLoading(true);
    setMessage(null);
    try {
      const tx = await api.transfer(
        { payerDocument: session.document, payeeDocument: digits, amount: value, password },
        session.token,
        `${Date.now()}-${Math.random().toString(16).slice(2)}`
      );
      setPassword("");
      setMessage(`Transferência ${tx.status} de ${formatBRL(tx.amount)}.`);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Falha na transferência");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <Display>Transferir</Display>
      <Muted>Informe o CPF/CNPJ do recebedor, o valor e a senha da sua conta.</Muted>
      <Field
        label="CPF / CNPJ do recebedor"
        keyboardType="number-pad"
        value={payee}
        onChangeText={setPayee}
        placeholder="39053344705"
      />
      <Field label="Valor" value={amount} onChangeText={setAmount} placeholder="1.234,56" />
      <Field
        label="Senha da conta"
        secureTextEntry
        autoCapitalize="none"
        autoCorrect={false}
        textContentType="password"
        value={password}
        onChangeText={setPassword}
      />
      <Alert message={message} />
      <PrimaryButton label={loading ? "Enviando…" : "Confirmar"} onPress={() => void onSubmit()} disabled={loading} />
    </Screen>
  );
}
