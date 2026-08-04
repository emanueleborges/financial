"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { ApiClientError, createUser } from "@/lib/api";
import { useAuth } from "@/components/auth-provider";
import { MoneyInput } from "@/components/money-input";
import { parseBRLInput } from "@/lib/money";
import {
  FormError,
  Field,
  inputClass,
  inputErrorClass,
  buttonPrimaryClass,
  buttonGhostClass,
} from "@/components/ui";

function onlyDigits(value: string) {
  return value.replace(/\D/g, "");
}

function validateDocument(raw: string): string | undefined {
  const digits = onlyDigits(raw);
  if (!digits) return "CPF/CNPJ é obrigatório";
  if (digits.length !== 11 && digits.length !== 14) {
    return "CPF deve ter 11 dígitos ou CNPJ 14 dígitos";
  }
  return undefined;
}

export default function RegisterPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [document, setDocument] = useState("");
  const [password, setPassword] = useState("");
  const [balanceMasked, setBalanceMasked] = useState("1.000,00");
  const [balanceValue, setBalanceValue] = useState<number | null>(1000);
  const [error, setError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  function validateLocal(): Record<string, string> {
    const next: Record<string, string> = {};
    if (name.trim().length < 2) next.name = "Nome deve ter pelo menos 2 caracteres";
    if (!email.includes("@")) next.email = "E-mail inválido";
    const docError = validateDocument(document);
    if (docError) next.document = docError;
    if (password.length < 6) next.password = "Senha deve ter pelo menos 6 caracteres";
    const balance = balanceValue ?? parseBRLInput(balanceMasked);
    if (balance == null || balance < 0) {
      next.initialBalance = "Saldo inicial não pode ser negativo";
    }
    return next;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const localErrors = validateLocal();
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) {
      setError(
        new ApiClientError(400, {
          code: "VALIDATION_ERROR",
          message: "Um ou mais campos são inválidos",
          fields: localErrors,
        })
      );
      return;
    }

    setLoading(true);
    try {
      const balance = balanceValue ?? parseBRLInput(balanceMasked) ?? 0;
      await createUser({
        name: name.trim(),
        email: email.trim(),
        document: onlyDigits(document),
        password,
        initialBalance: balance,
      });
      await login(onlyDigits(document), password);
      router.push("/app");
    } catch (err) {
      setError(err);
      if (err instanceof ApiClientError) {
        setFieldErrors(err.fields);
      } else {
        setFieldErrors({});
      }
    } finally {
      setLoading(false);
    }
  }

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
        <h1 className="font-[family-name:var(--font-display)] text-4xl text-[var(--foam)]">
          Criar conta
        </h1>
        <p className="mt-2 text-sm text-white/55">
          Cadastro com saldo inicial. CPF (11 dígitos) ou CNPJ (14 dígitos).
        </p>
        <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
          <Field label="Nome" error={fieldErrors.name}>
            <input
              className={fieldErrors.name ? inputErrorClass : inputClass}
              required
              minLength={2}
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.name;
                  return next;
                });
              }}
            />
          </Field>
          <Field label="E-mail" error={fieldErrors.email}>
            <input
              className={fieldErrors.email ? inputErrorClass : inputClass}
              type="email"
              required
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.email;
                  return next;
                });
              }}
            />
          </Field>
          <Field label="CPF / CNPJ" error={fieldErrors.document}>
            <input
              className={fieldErrors.document ? inputErrorClass : inputClass}
              required
              inputMode="numeric"
              maxLength={18}
              placeholder="52998224725"
              value={document}
              onChange={(e) => {
                setDocument(e.target.value);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.document;
                  return next;
                });
              }}
              aria-invalid={Boolean(fieldErrors.document)}
            />
          </Field>
          <Field label="Senha" error={fieldErrors.password}>
            <input
              className={fieldErrors.password ? inputErrorClass : inputClass}
              type="password"
              required
              minLength={6}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.password;
                  return next;
                });
              }}
            />
          </Field>
          <Field label="Saldo inicial" error={fieldErrors.initialBalance}>
            <MoneyInput
              error={Boolean(fieldErrors.initialBalance)}
              value={balanceMasked}
              placeholder="0,00"
              min={0}
              onChange={(masked, numeric) => {
                setBalanceMasked(masked);
                setBalanceValue(numeric);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.initialBalance;
                  return next;
                });
              }}
            />
          </Field>
          <FormError error={error} onDismiss={() => setError(null)} />
          <div className="flex flex-wrap gap-3 pt-2">
            <button type="submit" disabled={loading} className={buttonPrimaryClass}>
              {loading ? "Criando…" : "Criar conta"}
            </button>
            <Link href="/login" className={buttonGhostClass}>
              Já tenho conta
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
