package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;

import java.util.List;

/**
 * Mulige valg for en informasjonsbrevmal.
 * @param malType
 * @param mottakere
 * @param støtterFritekst
 * @param støtterTittelOgFritekst
 */
public record InformasjonsbrevValgDto(
    @JsonProperty("malType")
    InformasjonsbrevMalType malType,

    @JsonProperty("mottakere")
    List<InformasjonsbrevMottakerValgDto> mottakere,

    @JsonProperty("støtterFritekst")
    boolean støtterFritekst,

    @JsonProperty("støtterTittelOgFritekst")
    boolean støtterTittelOgFritekst
) {
}
