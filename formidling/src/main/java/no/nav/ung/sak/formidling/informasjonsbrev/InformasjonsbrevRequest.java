package no.nav.ung.sak.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevInnholdDto;

public record InformasjonsbrevRequest(
    Long behandlingId,
    InformasjonsbrevMalType informasjonsbrevMalType,
    InformasjonsbrevInnholdDto innhold,
    Boolean kunHtml
) {

    public <B extends InformasjonsbrevInnholdDto> B getTypedInnhold() {
        return (B) innhold;
    }
}
