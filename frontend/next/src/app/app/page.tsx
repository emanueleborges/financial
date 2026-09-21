"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { getBalance, getUser, type BalanceResponse, type UserResponse } from "@/lib/api";
import { formatBRL } from "@/lib/money";
import { FormError, buttonGhostClass } from "@/components/ui";

export default function DashboardPage() {
  const { session } = useAuth();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [balance, setBalance] = useState<BalanceResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const [u, b] = await Promise.all([
        getUser(session.document, session.token),
        getBalance(session.document, session.token),
      ]);
      setUser(u);
      setBalance(b);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.16em] text-white/45">Sua conta</p>
          <h1 className="mt-2 font-[family-name:var(--font-display)] text-4xl text-[var(--foam)]">
            {user?.name ?? "…"}
          </h1>
        </div>
        <button type="button" onClick={() => void load()} className={buttonGhostClass}>
          Atualizar
        </button>
      </div>

      <FormError error={error} onDismiss={() => setError(null)} />

      {loading && !user ? (
        <p className="text-white/50">Carregando saldo…</p>
      ) : (
        <section className="relative overflow-hidden rounded-xl border border-white/10 bg-white/[0.03] px-5 py-4">
          <p className="text-[10px] uppercase tracking-[0.14em] text-white/45">Saldo disponível</p>
          <p className="mt-1 font-[family-name:var(--font-display)] text-xl text-[var(--amber)] sm:text-2xl">
            {balance ? formatBRL(balance.balance) : "—"}
          </p>
          <dl className="mt-5 grid gap-4 text-sm text-white/65 sm:grid-cols-3">
            <div>
              <dt className="text-white/40">CPF / CNPJ</dt>
              <dd className="mt-1 font-mono text-sm">{user?.document ?? session?.document}</dd>
            </div>
            <div>
              <dt className="text-white/40">E-mail</dt>
              <dd className="mt-1">{user?.email}</dd>
            </div>
            <div>
              <dt className="text-white/40">Limite diário</dt>
              <dd className="mt-1">{user ? formatBRL(user.dailyLimit) : "—"}</dd>
            </div>
          </dl>
        </section>
      )}
    </div>
  );
}
