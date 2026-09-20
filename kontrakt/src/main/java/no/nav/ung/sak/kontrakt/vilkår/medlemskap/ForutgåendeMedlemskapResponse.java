package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ForutgåendeMedlemskapResponse(
    @JsonProperty(required = true)
    @NotNull
    @Valid
    List<MedlemskapPeriodeInfoDto> perioder
) {
}
