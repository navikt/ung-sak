package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;

import java.time.LocalDate;

/**
 * Brukt i response for å angi mottaker valg
 *
 * @param id                 - id til mottaker, kan være aktørId eller orgnr
 * @param idType
 * @param fødselsdato
 * @param navn               - navn på mottaker for visning
 * @param utilgjengeligÅrsak
 */
public record InformasjonsbrevMottakerValgResponse(
    String id,
    IdType idType,
    LocalDate fødselsdato,
    String navn,
    UtilgjengeligÅrsak utilgjengeligÅrsak
) {
}
