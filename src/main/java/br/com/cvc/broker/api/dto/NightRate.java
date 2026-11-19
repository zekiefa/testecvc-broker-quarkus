package br.com.cvc.broker.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Noite líquida: a mesma em cotação e em busca. */
public record NightRate(LocalDate date, BigDecimal adult, BigDecimal child) {
}
