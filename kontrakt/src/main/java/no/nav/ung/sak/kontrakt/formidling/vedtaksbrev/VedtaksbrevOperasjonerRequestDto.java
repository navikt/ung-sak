package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

/**
 *
 * @param behandlingId
 * @param hindret - hindre sending av brev
 * @param redigert - overstyre eller skrive fritekst vedtaksbrev
 * @param redigertHtml - html med tekst som skal overstyre
 */
public record VedtaksbrevOperasjonerRequestDto(
    @JsonProperty(value = BehandlingIdDto.NAME, required = true)
    @NotNull
    @Valid
    @AbacAttributt(BehandlingIdDto.NAME)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    @JsonProperty("hindret")
    Boolean hindret,

    @JsonProperty("redigert")
    Boolean redigert,

    @JsonProperty("redigertHtml")
    String redigertHtml
    ) {

}




