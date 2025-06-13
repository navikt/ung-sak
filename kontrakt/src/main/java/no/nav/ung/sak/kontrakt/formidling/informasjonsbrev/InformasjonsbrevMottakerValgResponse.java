package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;

/**
 * Brukt i response for å angi mottaker valg
 * @param id - id til mottaker, kan være aktørId eller orgnr
 * @param idType
 * @param navn - navn på mottaker for visning
 * @param fnr - hvis idType = aktørId så følger med fnr for visning
 * @param utilgjengeligÅrsak
 */
public record InformasjonsbrevMottakerValgResponse(
    String id,
    IdType idType,
    String navn,
    String fnr,
    UtilgjengeligÅrsak utilgjengeligÅrsak
) {
}
