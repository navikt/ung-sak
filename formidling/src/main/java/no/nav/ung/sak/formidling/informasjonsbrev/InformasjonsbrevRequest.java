package no.nav.ung.sak.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;

public record InformasjonsbrevRequest(
    Long behandlingId,
    InformasjonsbrevMalType informasjonsbrevMalType,
    GenereltFritekstBrevDto fritekstbrev,
    Boolean kunHtml
) {
}
