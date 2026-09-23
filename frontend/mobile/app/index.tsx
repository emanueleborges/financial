import { router } from "expo-router";
import { Brand, Display, GhostButton, Muted, PrimaryButton, Screen } from "../src/ui";

export default function Landing() {
  return (
    <Screen scroll={false}>
      <Brand size={22} />
      <Display size={48}>Financial Hub</Display>
      <Muted>
        Transferências P2P instantâneas com consistência financeira e rastreabilidade total.
      </Muted>
      <Muted>No celular: senha, Face ID / digital e selfie de reconhecimento facial.</Muted>
      <PrimaryButton label="Entrar" onPress={() => router.push("/login")} />
      <GhostButton label="Criar conta" onPress={() => router.push("/register")} />
    </Screen>
  );
}
