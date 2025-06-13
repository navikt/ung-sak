package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

/**
 * Request DTO for bestilling av informasjonsbrev.
 *
 * @param behandlingId Behandlingid.
 * @param dokumentMalType Angir malen som ønskes forhåndsvist eller bestilt.
 * @param innhold Kun satt for Generelt fritekstbrev.
 * @param mottaker Mottaker av informasjonsbrevet.
 */
public record InformasjonsbrevBestillingDto(
    @NotNull
    @Valid
    @AbacAttributt(BehandlingIdDto.NAME)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    @Valid
    DokumentMalType dokumentMalType,

    @Valid
    InformasjonsbrevMottakerDto mottaker,

    @Valid
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "dokumentMalType")
    InformasjonsbrevInnholdDto innhold
) {
}
