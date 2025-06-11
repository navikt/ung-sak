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
 * DTO for bestilling av informasjonsbrev.
 *
 * @param behandlingId Behandlingid.
 * @param informasjonsbrevMalType Angir malen som ønskes forhåndsvist eller bestilt.
 * @param innhold Kun satt for Generelt fritekstbrev.
 */
public record InformasjonsbrevBestillingDto(
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

    @Valid
    InformasjonsbrevMottakerDto mottakerDto,


    @JsonProperty("innhold")
    @Valid
    InformasjonsbrevInnholdDto innhold
) {
}
