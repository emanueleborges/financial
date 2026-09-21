import Link from "next/link";

export default function HomePage() {
  return (
    <div className="relative min-h-screen overflow-hidden bg-[var(--ink)]">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_120%_80%_at_10%_-10%,rgba(20,90,85,0.55),transparent_55%),radial-gradient(ellipse_80%_60%_at_100%_20%,rgba(232,168,56,0.18),transparent_50%),linear-gradient(180deg,#07131f_0%,#0a1c28_100%)]" />
      <div
        className="absolute inset-0 opacity-[0.07]"
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E\")",
        }}
      />

      <main className="relative z-10 mx-auto flex min-h-screen max-w-5xl flex-col justify-center px-6 py-16">
        <p className="mb-4 font-[family-name:var(--font-display)] text-5xl tracking-tight text-[var(--foam)] sm:text-7xl md:text-8xl">
          Financial Hub
        </p>
        <h1 className="max-w-xl font-[family-name:var(--font-body)] text-lg text-white/70 sm:text-xl">
          Transferências P2P instantâneas com consistência financeira e
          rastreabilidade total.
        </h1>
        <div className="mt-10 flex flex-wrap gap-3">
          <Link
            href="/login"
            className="inline-flex rounded-lg bg-[var(--amber)] px-6 py-3 text-sm font-semibold text-[var(--ink)] transition hover:bg-[#f0b84a]"
          >
            Entrar
          </Link>
          <Link
            href="/register"
            className="inline-flex rounded-lg border border-white/25 px-6 py-3 text-sm text-white/85 transition hover:bg-white/5"
          >
            Criar conta
          </Link>
        </div>
      </main>
    </div>
  );
}
