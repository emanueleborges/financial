"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/components/auth-provider";

const links = [
  { href: "/app", label: "Saldo" },
  { href: "/app/transfer", label: "Transferir" },
  { href: "/app/transactions", label: "Extrato" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const { session, ready, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (ready && !session) {
      router.replace("/login");
    }
  }, [ready, session, router]);

  if (!ready || !session) {
    return (
      <div className="grid min-h-screen place-items-center bg-[var(--ink)] text-[var(--foam)]">
        <p className="font-[family-name:var(--font-body)] text-sm tracking-wide opacity-70">
          Carregando…
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--ink)] text-[var(--foam)]">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(ellipse_at_top_right,rgba(20,90,85,0.22),transparent_50%),radial-gradient(ellipse_at_bottom_left,rgba(232,168,56,0.08),transparent_45%)]" />
      <header className="relative z-10 border-b border-white/10">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-6 py-5">
          <Link href="/app" className="font-[family-name:var(--font-display)] text-2xl tracking-tight">
            Financial Hub
          </Link>
          <nav className="flex flex-wrap items-center gap-1">
            {links.map((link) => {
              const active = pathname === link.href;
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`rounded-md px-3 py-2 text-sm transition ${
                    active
                      ? "bg-white/10 text-[var(--amber)]"
                      : "text-white/70 hover:bg-white/5 hover:text-white"
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
            <button
              type="button"
              onClick={() => {
                logout();
                router.push("/");
              }}
              className="ml-2 rounded-md px-3 py-2 text-sm text-white/50 hover:text-white"
            >
              Sair
            </button>
          </nav>
        </div>
      </header>
      <main className="relative z-10 mx-auto max-w-5xl px-6 py-10">{children}</main>
    </div>
  );
}
