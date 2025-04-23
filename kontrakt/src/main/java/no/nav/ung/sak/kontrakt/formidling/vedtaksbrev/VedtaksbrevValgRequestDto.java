package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

/**
 * @param behandlingId
 * @param hindret      - hindre sending av brev
 * @param redigert     - overstyre eller skrive fritekst vedtaksbrev
 * @param redigertHtml - html med tekst som skal overstyre
 */
public record VedtaksbrevValgRequestDto(
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
    @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String redigertHtml
) {

    @AssertTrue(message = "Redigert tekst kan ikke være tom samtidig som redigert er true")
    public boolean måHaTekstHvisRedigertErTrue() {
        return !Boolean.TRUE.equals(redigert) || (redigertHtml != null && !redigertHtml.isBlank());
    }

    @AssertTrue(message = "Kan ikke ha redigert tekst samtidig som redigert er false")
    public boolean kanIkkeHaTekstHvisRedigertErFalse() {
        return redigert != null && (redigert || redigertHtml == null);
    }

}




