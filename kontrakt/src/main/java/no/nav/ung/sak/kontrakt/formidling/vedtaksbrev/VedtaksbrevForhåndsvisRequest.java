package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

/**
 * DTO for forhåndsvisning av vedtaksbrev.
 *
 * @param behandlingId    Behandlingid.
 * @param redigertVersjon Angir om automatisk vedtaksbrev (false) eller lagret redigert brev skal forhåndsvises (true), default er false.
 *                        For fritekstbrev vil den alltid bruke redigert versjon.
 * @param htmlVersjon     Angir om html versjon skal hentes. False eller null henter PDF.
 * @param dokumentMalType Angier hvilket vedtaksbrev ønskes bestilt, null for default.
 */
public record VedtaksbrevForhåndsvisRequest(
    @NotNull
    @Valid
    @AbacAttributt(BehandlingIdDto.NAME)
    @Min(0)
    @Max(Long.MAX_VALUE)
    Long behandlingId,

    @Valid
    Boolean redigertVersjon,

    @Valid
    Boolean htmlVersjon,

    @Valid //TODO hør med Hallvard om ok å gjøre denne påkrevt
    DokumentMalType dokumentMalType) {

}
