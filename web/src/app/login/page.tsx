"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState, type ReactNode } from "react";
import { useAuth } from "@/components/auth-provider";
import {
  FormError,
  Field,
  inputClass,
  buttonPrimaryClass,
  buttonGhostClass,
} from "@/components/ui";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [document, setDocument] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(document.replace(/\D/g, ""), password);
      router.push("/app");
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthScreen>
      <h1 className="font-[family-name:var(--font-display)] text-4xl text-[var(--foam)]">
        Entrar
      </h1>
      <p className="mt-2 text-sm text-white/55">Acesse com seu CPF ou CNPJ.</p>
      <form onSubmit={onSubmit} className="mt-8 space-y-5">
        <Field label="CPF / CNPJ">
          <input
            className={inputClass}
            type="text"
            required
            inputMode="numeric"
            maxLength={18}
            value={document}
            onChange={(e) => setDocument(e.target.value)}
            autoComplete="username"
            placeholder="52998224725"
          />
        </Field>
        <Field label="Senha">
          <input
            className={inputClass}
            type="password"
            required
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </Field>
        <FormError error={error} onDismiss={() => setError(null)} />
        <div className="flex flex-wrap gap-3 pt-2">
          <button type="submit" disabled={loading} className={buttonPrimaryClass}>
            {loading ? "Entrando…" : "Entrar"}
          </button>
          <Link href="/register" className={buttonGhostClass}>
            Criar conta
          </Link>
        </div>
      </form>
    </AuthScreen>
  );
}

function AuthScreen({ children }: { children: ReactNode }) {
  return (
    <div className="relative min-h-screen bg-[var(--ink)]">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,rgba(20,90,85,0.35),transparent_55%)]" />
      <div className="relative z-10 mx-auto flex min-h-screen max-w-md flex-col justify-center px-6 py-12">
        <Link
          href="/"
          className="mb-10 font-[family-name:var(--font-display)] text-2xl text-[var(--foam)]"
        >
          Financial Hub
        </Link>
        {children}
      </div>
    </div>
  );
}
