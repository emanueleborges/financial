#!/usr/bin/env python3
"""Mede latência e vazão de POST /api/v1/transactions no Financial Hub local.

Cria N usuários (CPF válido, saldo alto), faz login uma vez por usuário e dispara transferências
concorrentes com Idempotency-Key única. Mede só a transferência (o login fica fora da conta).
"""
import argparse, json, statistics, time, urllib.error, urllib.request, uuid
from concurrent.futures import ThreadPoolExecutor

SENHA = "senha123"


def pedido(base, metodo, caminho, corpo=None, cabecalhos=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    h = {"Accept": "application/json", **({"Content-Type": "application/json"} if corpo is not None else {}), **(cabecalhos or {})}
    req = urllib.request.Request(base + caminho, data=dados, headers=h, method=metodo)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            texto = r.read().decode()
            return r.status, json.loads(texto) if texto else {}
    except urllib.error.HTTPError as e:
        return e.code, {}
    except (TimeoutError, urllib.error.URLError, ConnectionError):
        return 0, {}  # 0 = timeout ou conexão recusada


def cpf(semente):
    d = [int(c) for c in f"{semente % 1_000_000_000:09d}"]
    for peso in (10, 11):
        s = sum((peso - i) * v for i, v in enumerate(d)) % 11
        d.append(0 if s < 2 else 11 - s)
    return "".join(map(str, d))


def criar(base, i, prefixo):
    doc = cpf(prefixo + i)
    st, _ = pedido(base, "POST", "/api/v1/users", {"name": f"Carga {i}", "email": f"carga-{prefixo}-{i}@example.test",
                                                  "document": doc, "password": SENHA, "initialBalance": 1_000_000.0})
    if st not in (201, 409):
        raise RuntimeError(f"usuário {i}: HTTP {st}")
    st, r = pedido(base, "POST", "/api/v1/auth/login", {"document": doc, "password": SENHA})
    if st != 200:
        raise RuntimeError(f"login {i}: HTTP {st}")
    return {"doc": doc, "token": r["accessToken"]}


def transferir(base, pagador, recebedor):
    inicio = time.perf_counter()
    st, _ = pedido(base, "POST", "/api/v1/transactions",
                   {"payerDocument": pagador["doc"], "payeeDocument": recebedor["doc"], "amount": 1.0, "password": SENHA},
                   {"Authorization": f"Bearer {pagador['token']}", "Idempotency-Key": str(uuid.uuid4())})
    return st, (time.perf_counter() - inicio) * 1000


def rodada(base, usuarios, total, conc):
    pares = [(usuarios[i % len(usuarios)], usuarios[(i + 1) % len(usuarios)]) for i in range(total)]
    inicio = time.perf_counter()
    with ThreadPoolExecutor(max_workers=conc) as ex:
        res = list(ex.map(lambda p: transferir(base, *p), pares))
    duracao = time.perf_counter() - inicio
    ok = sorted(ms for st, ms in res if st == 201)
    status = {}
    for st, _ in res:
        status[st] = status.get(st, 0) + 1
    q = statistics.quantiles(ok, n=100) if len(ok) >= 2 else [0] * 99
    return {"concorrencia": conc, "pedidos": total, "status": status, "ok": len(ok),
            "vazao_ok_por_s": round(len(ok) / duracao, 1), "duracao_s": round(duracao, 2),
            "p50_ms": round(q[49], 1), "p95_ms": round(q[94], 1), "p99_ms": round(q[98], 1)}


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--base", default="http://localhost:8080")
    a.add_argument("--usuarios", type=int, default=100)
    a.add_argument("--pedidos", type=int, default=2000)
    a.add_argument("--concorrencias", default="1,16,32")
    a.add_argument("--aquecimento", type=int, default=300)
    a.add_argument("--saida")
    args = a.parse_args()
    prefixo = 300_000_000 + int(time.time()) % 100_000 * 1000
    with ThreadPoolExecutor(max_workers=8) as ex:
        usuarios = list(ex.map(lambda i: criar(args.base, i, prefixo), range(args.usuarios)))
    print(f"usuários prontos: {len(usuarios)}")
    rodada(args.base, usuarios, args.aquecimento, 16)  # aquece JIT, pools e cache
    resultados = []
    for conc in map(int, args.concorrencias.split(",")):
        total = args.pedidos if conc > 1 else min(args.pedidos, 300)
        r = rodada(args.base, usuarios, total, conc)
        print(json.dumps(r, ensure_ascii=False))
        resultados.append(r)
    if args.saida:
        json.dump(resultados, open(args.saida, "w"), indent=2, ensure_ascii=False)


if __name__ == "__main__":
    main()
