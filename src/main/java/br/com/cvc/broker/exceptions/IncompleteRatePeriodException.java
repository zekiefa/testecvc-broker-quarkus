package br.com.cvc.broker.exceptions;

import java.time.LocalDate;
import java.util.List;

/** Periodo com noite sem tarifa publicada. Nunca cotar parcialmente: o comprador recebe o que falta. */
public class IncompleteRatePeriodException extends BrokerException {

    private final transient List<LocalDate> missingDates;

    public IncompleteRatePeriodException(final List<LocalDate> missingDates) {
        super("incomplete-rate-period", 409,
                        "No rate published for " + missingDates.size() + " night(s) in the period");
        this.missingDates = List.copyOf(missingDates);
    }

    public List<LocalDate> missingDates() {
        return this.missingDates;
    }
}
