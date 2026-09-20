package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ForutgåendeMedlemskapResponse(
    @JsonProperty(required = true)
    List<MedlemskapPeriodeInfoDto> perioder
) {
}
