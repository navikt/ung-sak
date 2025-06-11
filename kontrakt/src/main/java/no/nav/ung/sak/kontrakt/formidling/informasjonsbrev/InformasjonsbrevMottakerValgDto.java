package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;

/**
 * Brukt i response for å angi mottaker valg
 * @param id
 * @param idType
 * @param utilgjengeligÅrsak
 */
public record InformasjonsbrevMottakerValgDto(
    String id,
    IdType idType,
    UtilgjengeligÅrsak utilgjengeligÅrsak
) {
}
