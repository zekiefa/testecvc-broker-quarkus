---
project: testecvc-broker-quarkus
role: broker-feed               # lado do fornecedor (B2B)
doc_version: 1
updated: 2026-11-06
stack: Java 21 · Quarkus 3.40 · PostgreSQL 16 · Flyway
depends_on: []                  # não depende de nenhum outro serviço
consumed_by:
  - project: testecvc-quarkus
    uses: [GET /v1/search, GET /v1/hotels/{hotelId}, POST /v1/quotes]
    contract: openapi/broker-v1.yaml (v1)
owns: [catálogo de hotéis, quartos, regimes, tarifas por data, disponibilidade publicada, identidade B2B]
does_not_own: [reservas, estoque bloqueado, pagamento]
canonical:
  arquitetura: docs/arquitetura.md
  contrato: openapi/broker-v1.yaml   # servido em /q/openapi
---

# Capacidades de negócio — broker

> **Leia o TL;DR antes de qualquer outra coisa.** Cite IDs (`BRK-03`, `B-05`) em vez de
> reescrever a capacidade.

## TL;DR

- Publica **catálogo, tarifas por data e disponibilidade** para um comprador (hoje, um só).
- **Não vende e não reserva**: não bloqueia estoque, não confirma reserva, não cobra.
- Todo preço é **líquido**: a margem do comprador não entra no número.
- Todo preço carrega **frescor** (`updatedAt`, `staleAfter`) e **origem** (`SEED` ou `PUSH`).
- Autenticação é **API key B2B** com escopo `read` / `admin`.

## Capacidades — lado comprador (escopo `read`)

| ID | Capacidade | Endpoint | Retorna |
|---|---|---|---|
| BRK-01 | Buscar hotéis por destino com preço do período | `GET /v1/search?cityCode&checkIn&checkOut&adults&children` | 1 item **por hotel**: melhor plano, total líquido, **noites detalhadas**, `dataSource`, `staleAfter` |
| BRK-02 | Listar hotéis por cidade | `GET /v1/hotels?cityCode&limit&offset` | página de hotéis |
| BRK-03 | Catálogo do hotel | `GET /v1/hotels/{hotelId}` | quartos e regimes (sem preço) |
| BRK-04 | Cotar estadia | `POST /v1/quotes` | total líquido + noites + frescor |
| BRK-05 | Disponibilidade publicada | `GET /v1/availability?roomTypeId&checkIn&checkOut` | allotment por noite + mínimo do período |

Campos de `BRK-04`: `hotelId`, `roomTypeId`, `ratePlanId`, `checkIn`, `checkOut`, `adults`, `children`.

## Capacidades — lado fornecedor (escopo `admin`)

| ID | Capacidade | Endpoint | Efeito |
|---|---|---|---|
| BRK-06 | Publicar hotel | `PUT /admin/hotels/{hotelId}` | upsert de hotel, quartos e regimes; marca `dataSource: PUSH` |
| BRK-07 | Publicar tarifas | `PUT /admin/rooms/{roomTypeId}/rates` | upsert do intervalo (`from`..`to` inclusive); renova `updated_at` |
| BRK-08 | Publicar disponibilidade | `PUT /admin/rooms/{roomTypeId}/availability` | upsert de allotment e stop-sell |

## Invariantes de negócio

| ID | Regra | Violação |
|---|---|---|
| B-01 | Tarifa é chaveada por **data de estadia** (`stay_date`), não por faixa | — |
| B-02 | Cotar período com **qualquer noite sem tarifa** → `409 incomplete-rate-period` com `missingDates` | nunca total parcial |
| B-03 | **Frescor explícito**: toda resposta de preço traz `updatedAt` e `staleAfter` (TTL = `broker.rates.freshness-minutes`, padrão 15 min) | — |
| B-04 | **Origem declarada**: `dataSource ∈ {SEED, PUSH}` em toda resposta de preço | — |
| B-05 | Valores são **líquidos**; a remuneração do comprador não entra | — |
| B-06 | Capacidade do quarto é **limite do vendedor**: `adults > maxAdults` → `400 guest-limit-exceeded` | — |
| B-07 | Período máximo `broker.rates.max-nights` (**padrão 30**, ambiente `BROKER_MAX_NIGHTS`); faixa invertida → `400 invalid-period` | — |
| B-08 | **Noite não publicada** retorna `allotment: 0` + `stopSell: true` — silêncio não é estoque | — |
| B-09 | `minAllotment` = piso do período (o que pode ser vendido para **todas** as noites) | — |
| B-10 | Escopo vem do cadastro, nunca do header; ausente/inválida → `401`; sem escopo → `403 insufficient-scope` | — |
| B-11 | Chave é guardada como **SHA-256**; a chave em claro nunca é gravada nem devolvida | — |
| B-12 | Todo erro é `application/problem+json` (RFC 7807) com `type` estável e `traceId` | — |

## Contrato

- **Fonte única**: `openapi/broker-v1.yaml` — o build copia para `META-INF` e o serviço serve **esse arquivo** em `/q/openapi` (scan de anotações desligado). Documento e implementação não divergem.
- **Versionamento**: `/v1` evolui só por adição compatível.

> Documento equivalente do comprador: <https://github.com/zekiefa/testecvc-quarkus/blob/main/docs/ai/capabilities.md>

## Erros que o comprador pode receber

| `type` | HTTP | Quando |
|---|---|---|
| `hotel-not-found` · `room-not-found` · `rate-plan-not-found` | 404 | id inexistente ou combinação inválida |
| `invalid-period` | 400 | `checkOut` não posterior a `checkIn`, ou período > limite |
| `guest-limit-exceeded` | 400 | pax acima da capacidade do quarto |
| `validation-failed` | 400 | campo obrigatório ausente/fora de faixa |
| `incomplete-rate-period` | 409 | noite sem tarifa (com `missingDates`) |
| `unauthorized` | 401 | chave ausente/inválida |
| `insufficient-scope` | 403 | chave sem o escopo exigido |

## Dados e simulação

| Tabela | Dono | Observação |
|---|---|---|
| `hotel`, `room_type`, `rate_plan` | este serviço | `dataSource` na linha |
| `rate` (por `stay_date`) | este serviço | valores **líquidos de atacado** |
| `availability` | este serviço | allotment e stop-sell publicados |
| `api_client` | este serviço | hash da chave, escopos, `rate_limit_per_min` |

**Simulado hoje**: 10 hotéis de Serra Negra com tarifas geradas (`SEED`) e disponibilidade semeada. **Não existe** fornecedor real, PMS, agregação de múltiplos fornecedores.

## Não-objetivos (não proponha sem decisão explícita)

Reserva/hold de estoque · consumo de allotment · pagamento · agregação de fornecedores (fan-out) · conversão de moeda · precificação dinâmica · multi-tenant · enforcement de rate limit (o campo existe, a régua não) · integração com PMS.

## Como usar este arquivo (agentes)

1. Responda pelo **TL;DR** e pelas tabelas; o contrato completo está em `openapi/broker-v1.yaml`.
2. Cite IDs (`BRK-04`, `B-02`).
3. Lembre sempre: este serviço **publica**, não vende. Se a pergunta envolve vaga garantida ou reserva, ela pertence ao `testecvc-quarkus`.
4. Antes de propor nova capacidade, confira **Não-objetivos**.
