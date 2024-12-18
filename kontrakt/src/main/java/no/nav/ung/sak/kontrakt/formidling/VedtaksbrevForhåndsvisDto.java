package no.nav.ung.sak.kontrakt.formidling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record VedtaksbrevForhåndsvisDto(

    /**
     * Behandlingid
     */
    @JsonProperty(value = BehandlingIdDto.NAME, required = true)
    @NotNull
    @Valid
    @AbacAttributt(BehandlingIdDto.NAME)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    /**
     * For brev som støtter flere mottakere
     */
    @JsonProperty("mottaker")
    @Valid
    MottakerDto mottaker,

    /**
     * Angir om automatisk vedtaksbrev eller lagret manuell brev skal forhåndsvises, default er false
     */
    @JsonProperty("manuellBrev")
    @Valid
    Boolean manuellBrev,

    /**
     * TODO
     */
    @JsonProperty("dokumentdata")
    @Valid
    JsonNode dokumentdata


) {
}
