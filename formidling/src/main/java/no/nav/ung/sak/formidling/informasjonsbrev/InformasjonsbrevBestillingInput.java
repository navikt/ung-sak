package no.nav.ung.sak.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevInnholdDto;

public record InformasjonsbrevBestillingInput(
    Long behandlingId,
    DokumentMalType dokumentMalType,
    InformasjonsbrevInnholdDto innhold,
    boolean kunHtml
) {

    public <B extends InformasjonsbrevInnholdDto> B getTypedInnhold() {
        return (B) innhold;
    }
}
