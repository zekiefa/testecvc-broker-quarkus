package br.com.cvc.broker.exceptions;

import java.time.LocalDate;
import java.util.List;

/** Periodo com noite sem tarifa publicada. Nunca cotar parcialmente: o comprador recebe o que falta. */
public class IncompleteRatePeriodException extends BrokerException {

    private final transient List<LocalDate> missingDates;

    public IncompleteRatePeriodException(final List<LocalDate> missingDates) {
        // corpo de construtor flexível (Java 25): a cópia imutável acontece antes do super,
        // então o estado da exceção não depende de quem chamou mudar a lista depois.
        final var snapshot = List.copyOf(missingDates);
        super("incomplete-rate-period", 409,
                        "No rate published for " + snapshot.size() + " night(s) in the period");
        this.missingDates = snapshot;
    }

    public List<LocalDate> missingDates() {
        return this.missingDates;
    }
}
