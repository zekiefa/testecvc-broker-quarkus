# testecvc-broker-quarkus

> Broker de hotéis: publica **catálogo, tarifas e disponibilidade** com **frescor explícito** para
> que um comprador (o serviço de reservas) consiga cotar uma estadia sem inventar preço.

Repositório irmão: [testecvc-quarkus](https://github.com/zekiefa/testecvc-quarkus) — o serviço de reservas, que é o *buyer*.

| | |
|---|---|
| Estado | **contrato (OpenAPI) escrito**; sem código ainda |
| Escopo v1 | feed de catálogo/tarifas/disponibilidade (não vende, não reserva) |
| Stack | Java **25** (LTS) · Quarkus 3.40 · PostgreSQL 16 · Flyway |
| Contrato | [`openapi/broker-v1.yaml`](openapi/broker-v1.yaml) |
| Arquitetura | [`docs/arquitetura.md`](docs/arquitetura.md) |

## Por que este projeto existe

O serviço de reservas dependia de um broker que era apenas uma fixture com uma cidade fictícia.
Duas consequências medidas:

1. **Preço não era validável** — a tentativa de validação de mercado ficou inconclusiva por falta de comparável;
2. **O caminho de integração nunca foi exercitado** — contrato, frescor, falha de fornecedor e mapeamento de conteúdo nunca rodaram com dado realista.

## Regra que ordena o desenho

Este sistema morre vendendo **tarifa velha como se fosse atual**. Por isso:

- toda resposta de tarifa devolve `updatedAt` e `staleAfter`;
- toda resposta declara a **origem do dado** (`dataSource`: `SEED` ou `PUSH`) — dado sintético nunca se disfarça de real;
- tarifa é por `stay_date`; período com noite faltando é **409**, nunca total silenciosamente errado.

## Contrato primeiro

O `openapi/broker-v1.yaml` é o produto. Nenhuma classe Java antes dele.

```shell
# validar o contrato (quando o CLI estiver disponível)
npx --yes @redocly/cli lint openapi/broker-v1.yaml
```

## Roadmap (ver docs/arquitetura.md §5)

| Marco | Conteúdo |
|---|---|
| **MB0** | esqueleto + contrato publicado + API key + RFC 7807 + health/métricas |
| **MB1** | catálogo, tarifas, seed de Serra Negra, `POST /v1/quotes` |
| **MB2** | ingestão admin + disponibilidade + `staleAfter` real |
| **MB3** | integração com o serviço de reservas (substituir a fixture) |
| **MB4** | alocador (`hold`/`bookings`) — só com sinal |

## Documentação

- **[Capacidades para agentes de IA](docs/ai/capabilities.md)** — features de negócio, invariantes
  numeradas, contrato e fronteira de dados, em formato compacto (leia antes de varrer o código)
- [Arquitetura](docs/arquitetura.md) · [Contrato OpenAPI](openapi/broker-v1.yaml)

## Endpoints

Autenticação: header **`X-Api-Key`**. Em desenvolvimento: `read-key-dev` (escopo `read`) e
`admin-key-dev` (`read,admin`). O escopo exigido é derivado do caminho: `/admin/**` → `admin`;
o resto → `read`. Chave ausente ou inválida → `401`; escopo insuficiente → `403`.

Os IDs correspondem a [`docs/ai/capabilities.md`](docs/ai/capabilities.md).

### Comprador (escopo `read`)

| ID | Método | Caminho | Entrada | Sucesso | Erros |
|---|---|---|---|---|---|
| `BRK-01` | `GET` | `/v1/search` | `cityCode`, `checkIn`, `checkOut`, `adults`, `children` | `200` uma oferta **por hotel**: plano mais barato, total líquido, **noites detalhadas**, `dataSource`, `staleAfter` | `400` período/parâmetro · `401` |
| `BRK-02` | `GET` | `/v1/hotels` | `cityCode`, `limit`, `offset` | `200` página de hotéis (`items`, `page`) | `400` · `401` |
| `BRK-03` | `GET` | `/v1/hotels/{hotelId}` | path | `200` quartos e regimes (catálogo, **sem preço**) | `404` · `401` |
| `BRK-04` | `POST` | `/v1/quotes` | `{hotelId, roomTypeId, ratePlanId, checkIn, checkOut, adults, children}` | `200` total **líquido** + noites + `updatedAt`/`staleAfter` + `dataSource` | `400` período/validação/pax · `404` hotel/quarto/plano · `409` `incomplete-rate-period` (com `missingDates`) · `401` |
| `BRK-05` | `GET` | `/v1/availability` | `roomTypeId`, `checkIn`, `checkOut` | `200` allotment por noite + `minAllotment` + frescor | `400` · `404` · `401` |

### Fornecedor (escopo `admin`)

| ID | Método | Caminho | Entrada | Sucesso | Erros |
|---|---|---|---|---|---|
| `BRK-06` | `PUT` | `/admin/hotels/{hotelId}` | `{name, cityCode, cityName, roomTypes[]{id, name, maxAdults, maxChildren, ratePlans[]}}` | `200` hotel publicado; marca `dataSource: PUSH` | `400` · `403` · `401` |
| `BRK-07` | `PUT` | `/admin/rooms/{roomTypeId}/rates` | `{ratePlanId, currency, from, to, adultAmount, childAmount}` (intervalo **inclusive**) | `204` tarifas publicadas; renova `updated_at` | `400` faixa invertida · `403` · `404` quarto/plano · `401` |
| `BRK-08` | `PUT` | `/admin/rooms/{roomTypeId}/availability` | `{from, to, allotment, stopSell}` | `204` disponibilidade publicada | `400` · `403` · `404` · `401` |

### Operação

| ID | Método | Caminho | Sucesso |
|---|---|---|---|
| — | `GET` | `/q/openapi` | `200` — serve o **próprio** `openapi/broker-v1.yaml` |
| — | `GET` | `/q/health` | `200` saúde (inclui datasource) |
| — | `GET` | `/q/metrics` | `200` métricas Prometheus |

Corpo de erro: `application/problem+json` (RFC 7807) com `type` estável e `traceId`.
Tipos: `hotel-not-found`, `room-not-found`, `rate-plan-not-found`, `invalid-period`,
`guest-limit-exceeded`, `validation-failed`, `incomplete-rate-period`, `unauthorized`,
`insufficient-scope`.

> `429 rate-limited` está **descrito no contrato** mas ainda não é aplicado: a coluna
> `api_client.rate_limit_per_min` existe, a régua não. Ver "Não-objetivos" em
> [`docs/ai/capabilities.md`](docs/ai/capabilities.md).

### Exemplo

```shell
# publicar tarifa (admin) e cotar (buyer)
curl -s -X PUT localhost:8081/admin/rooms/1/rates -H 'X-Api-Key: admin-key-dev' \
  -H 'Content-Type: application/json' \
  -d '{"ratePlanId":1,"currency":"BRL","from":"2027-04-10","to":"2027-04-12",
       "adultAmount":900.00,"childAmount":450.00}'

curl -s -X POST localhost:8081/v1/quotes -H 'X-Api-Key: read-key-dev' \
  -H 'Content-Type: application/json' \
  -d '{"hotelId":1,"roomTypeId":1,"ratePlanId":1,
       "checkIn":"2027-04-10","checkOut":"2027-04-13","adults":1,"children":1}' | jq
```

## Como este repositório é gerenciado

- **Tracker:** as tarefas deste projeto vivem no Beads do repositório irmão
  ([`testecvc-quarkus`](https://github.com/zekiefa/testecvc-quarkus)), sob o épico `tcq-ptp`.
  Este repositório **não** tem `.beads` próprio, de propósito: dois quadros viram dois lugares
  para esquecer uma tarefa.
- **Consumidor do contrato:** o serviço de reservas. Mudança incompatível em `/v1` exige
  atualização lá também (`docs/arquitetura.md`, §3.5, e o `BrokerCatalog`).
- **Fonte da verdade:** aqui, o contrato e a arquitetura do broker; no outro repositório, a
  decisão de construí-lo e o consumidor.
- **Sessão de trabalho:** os dois repositórios são gerenciados pela mesma sessão; a raiz do
  workspace é o repositório de reservas, então **escritas aqui exigem aprovação de acesso amplo**.

## Licença

Projeto de estudo, sem licença definida.
