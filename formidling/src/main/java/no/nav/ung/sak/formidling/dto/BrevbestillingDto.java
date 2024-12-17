package no.nav.ung.sak.formidling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.Saksnummer;

public record BrevbestillingDto(
    @AbacAttributt("behandlingId")
    @JsonProperty(value = BehandlingIdDto.NAME, required = true)
    @NotNull
    @Valid
    Long behandlingId,

    @JsonProperty(value = "malType", required = true)
    @NotNull
    @Valid
    DokumentMalType malType,

    @AbacAttributt("saksnummer")
    @JsonProperty(value = SaksnummerDto.NAME, required = true)
    @NotNull
    @Valid
    Saksnummer saksnummer,

    /*
      overstyr mottaker hvis relevant - TODO vurder om frontend skal slippe det,
      mens backend alltid setter det
     */
    @JsonProperty(value = "mottaker")
    @Nullable
    @Valid
    PartRequestDto mottaker,

    /*
     * felter som skal injectes i malen //TODO endre til json subtypes
     */
    @JsonProperty("dokumentdata")
    JsonNode dokumentdata
) {
}
