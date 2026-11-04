package br.com.cvc.broker.api.dto;

import java.util.List;

/** Pagina de hoteis. {@code total} conta o que casa com o filtro, nao o tamanho da pagina. */
public record HotelPage(List<HotelSummary> items, Page page) {

    public record HotelSummary(Long id, String name, String cityCode, String cityName, String dataSource,
                    java.time.Instant updatedAt) {
    }

    public record Page(int limit, int offset, long total) {
    }
}
