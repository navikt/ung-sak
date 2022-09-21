package no.nav.k9.sak.kontrakt.vedtak;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;

public class LosVedtaksdataDto {
    //vedtaksdato
    //vedtakId
    //datoForUttak

    @JsonProperty(value = "vedtakstidspunkt")
    @Valid
    private LocalDateTime vedtakstidspunkt;

    @JsonProperty(value = "vedtakId")
    @Valid
    private Long vedtakId;

    public LosVedtaksdataDto(LocalDateTime vedtakstidspunkt, Long vedtakId) {
        this.vedtakstidspunkt = vedtakstidspunkt;
        this.vedtakId = vedtakId;
    }
}
