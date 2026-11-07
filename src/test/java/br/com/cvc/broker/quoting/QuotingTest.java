package br.com.cvc.broker.quoting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.cvc.broker.catalogue.RateRepository.NightRateRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste <b>puro</b> da soma de diárias: sem Quarkus, sem banco, sem broker.
 *
 * <p>Serve a dois propósitos: roda em milissegundos e é o que o PIT consegue usar — mutar
 * classes que precisam de container mede o container, não a aritmética.</p>
 */
class QuotingTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2027, 4, 10);

    @Test
    @DisplayName("Uma noite, 1 adulto + 1 criança")
    void oneNightAddsBothPax() {
        assertEquals(money("1350.00"), Quoting.total(nights("900.00", "450.00", 1), 1, 1));
    }

    @Test
    @DisplayName("Três noites somam três diárias, não uma")
    void threeNightsSum() {
        assertEquals(money("4050.00"), Quoting.total(nights("900.00", "450.00", 3), 1, 1));
    }

    @Test
    @DisplayName("Dois adultos custam o dobro do adulto")
    void twoAdultsDoubleTheAdultShare() {
        assertEquals(money("1800.00"), Quoting.total(nights("900.00", "450.00", 1), 2, 0), "900 x 2, sem criança");
    }

    @Test
    @DisplayName("Sem pax o total é zero — nunca um valor inventado")
    void zeroPaxCostsZero() {
        assertEquals(money("0.00"), Quoting.total(nights("900.00", "450.00", 2), 0, 0));
    }

    @Test
    @DisplayName("Período vazio custa zero")
    void emptyPeriodCostsZero() {
        assertEquals(money("0.00"), Quoting.total(List.of(), 1, 1));
    }

    @Test
    @DisplayName("Arredonda uma vez no fim, HALF_UP, escala 2")
    void roundsHalfUpAtTheEnd() {
        final var total = Quoting.total(nights("100.555", "0.00", 1), 1, 0);

        assertEquals(money("100.56"), total);
        assertEquals(2, total.scale(), "escala monetária é sempre 2");
    }

    private static List<NightRateRow> nights(final String adult, final String child, final int count) {
        return java.util.stream.IntStream.range(0, count)
                        .mapToObj(offset -> new NightRateRow(CHECK_IN.plusDays(offset), new BigDecimal(adult),
                                        new BigDecimal(child), "BRL", Instant.parse("2026-11-06T12:00:00Z")))
                        .toList();
    }

    private static BigDecimal money(final String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}
