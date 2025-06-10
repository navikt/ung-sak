package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;

public record InformasjonsbrevMottakerDto(
    String id,
    IdType idType,
    UtilgjengeligÅrsak utilgjengeligÅrsak
) {
}
