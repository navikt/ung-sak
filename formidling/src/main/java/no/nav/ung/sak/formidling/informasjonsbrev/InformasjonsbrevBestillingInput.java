package no.nav.ung.sak.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevInnholdDto;

public record InformasjonsbrevBestillingInput(
    Long behandlingId,
    InformasjonsbrevMalType informasjonsbrevMalType,
    InformasjonsbrevInnholdDto innhold,
    boolean kunHtml
) {

    public <B extends InformasjonsbrevInnholdDto> B getTypedInnhold() {
        return (B) innhold;
    }
}
