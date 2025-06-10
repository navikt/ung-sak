package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

/**
 * DTO for forhåndsvisning av informasjonsbrev.
 *
 * @param behandlingId Behandlingid.
 * @param informasjonsbrevMalType Angir malen som ønskes å forhåndsvises.
 * @param fritekstbrev Kun satt for Generelt fritekstbrev.
 * @param htmlVersjon Angir om html versjon skal hentes. False eller null henter PDF.
 */
public record InformasjonsbrevForhåndsvisDto(
    @JsonProperty(value = BehandlingIdDto.NAME, required = true)
    @NotNull
    @Valid
    @AbacAttributt(BehandlingIdDto.NAME)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    @JsonProperty("informasjonsbrevMalType")
    @Valid
    InformasjonsbrevMalType informasjonsbrevMalType,

    @JsonProperty("fritekstbrev")
    GenereltFritekstBrevDto fritekstbrev,

    @JsonProperty("htmlVersjon")
    @Valid
    Boolean htmlVersjon
) {
}
