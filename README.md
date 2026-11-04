# testecvc-broker-quarkus

> Broker de hotéis: publica **catálogo, tarifas e disponibilidade** com **frescor explícito** para
> que um comprador (o serviço de reservas) consiga cotar uma estadia sem inventar preço.

Repositório irmão: [testecvc-quarkus](https://github.com/zekiefa/testecvc-quarkus) — o serviço de reservas, que é o *buyer*.

| | |
|---|---|
| Estado | **contrato (OpenAPI) escrito**; sem código ainda |
| Escopo v1 | feed de catálogo/tarifas/disponibilidade (não vende, não reserva) |
| Stack prevista | Java 21 · Quarkus · PostgreSQL · Flyway |
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

## Licença

Projeto de estudo, sem licença definida.
