package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.KodeverdiSomObjekt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;

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
    KodeverdiSomObjekt<DokumentMalType> malType,

    @JsonProperty("mottakere")
    List<InformasjonsbrevMottakerValgDto> mottakere,

    @JsonProperty("støtterFritekst")
    boolean støtterFritekst,

    @JsonProperty("støtterTittelOgFritekst")
    boolean støtterTittelOgFritekst
) {
}
