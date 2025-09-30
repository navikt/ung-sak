package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.kontrakt.Patterns;

/**
 * @param behandlingId
 * @param hindret         - hindre sending av brev
 * @param redigert        - overstyre eller skrive fritekst vedtaksbrev
 * @param redigertHtml    - html med tekst som skal overstyre
 * @param dokumentMalType - malen valgene gjelder for
 */
public record VedtaksbrevValgRequest(
    @NotNull
    @Valid
    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_ID)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,
    Boolean hindret,
    Boolean redigert,

    @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String redigertHtml,

    @NotNull
    @Valid
    DokumentMalType dokumentMalType) {

    @AssertTrue(message = "Redigert tekst kan ikke være tom samtidig som redigert er true")
    public boolean isEmptyRedigertTekstAndRedigertTrue() {
        return !Boolean.TRUE.equals(redigert) || (redigertHtml != null && !redigertHtml.isBlank());
    }

    @AssertTrue(message = "Kan ikke ha redigert tekst samtidig som redigert er false")
    public boolean isRedigertTextAndRedigertFalse() {
        return redigert != null && (redigert || redigertHtml == null);
    }

}




