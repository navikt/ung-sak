package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public record InformasjonsbrevMottakerValgDto(
    @JsonProperty("id")
    String id,

    @JsonProperty("idType")
    IdType idType,

    @JsonProperty("navn")
    String navn,

    @JsonProperty("fnr")
    String fnr,

    @JsonProperty("utilgjengeligÅrsak")
    UtilgjengeligÅrsak utilgjengeligÅrsak
) {
}
