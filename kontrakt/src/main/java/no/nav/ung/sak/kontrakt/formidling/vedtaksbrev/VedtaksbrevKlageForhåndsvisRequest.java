package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;

/**
 * DTO for forhåndsvisning av klage vedtaksbrev.
 *
 * @param behandlingId    Behandlingid.
 * @param htmlVersjon     Angir om html versjon skal hentes. False eller null henter PDF.
 */
public record VedtaksbrevKlageForhåndsvisRequest(
    @NotNull
    @Valid
    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_ID)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    @Valid
    Boolean htmlVersjon
) {

}
