package br.com.cvc.broker.quoting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import br.com.cvc.broker.catalogue.RateRepository.NightRateRow;

/** Soma de diarias. Puro: sem CDI, sem I/O, sem banco — o dinheiro precisa ser deterministico. */
public final class Quoting {

    private Quoting() {
    }

    /**
     * Total do periodo: por noite, {@code adulto × adultos + crianca × criancas}.
     *
     * <p>Arredonda uma vez, no fim, em {@code HALF_UP} escala 2 — arredondar por noite
     * acumularia centavo de diferenca entre detalhe e total.</p>
     */
    public static BigDecimal total(final List<NightRateRow> nights, final int adults, final int children) {
        final var adultCount = BigDecimal.valueOf(adults);
        final var childCount = BigDecimal.valueOf(children);

        return nights.stream()
                        .map(night -> night.adult().multiply(adultCount).add(night.child().multiply(childCount)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
    }
}
