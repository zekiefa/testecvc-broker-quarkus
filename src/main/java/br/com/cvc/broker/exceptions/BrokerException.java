package br.com.cvc.broker.exceptions;

/**
 * Erro de dominio do broker, carregando o {@code type} que vai no problem+json.
 *
 * <p>O slug e o contrato: o cliente programa contra ele, entao ele nao muda por refatoracao
 * de classe.</p>
 */
public class BrokerException extends RuntimeException {

    private final String slug;
    private final int status;

    public BrokerException(final String slug, final int status, final String detail) {
        super(detail);
        this.slug = slug;
        this.status = status;
    }

    public String slug() {
        return this.slug;
    }

    public int status() {
        return this.status;
    }
}
