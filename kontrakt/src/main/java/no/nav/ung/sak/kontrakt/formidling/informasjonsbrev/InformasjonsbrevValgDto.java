package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

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
    KodeverdiSomObjekt<DokumentMalType> malType,
    List<InformasjonsbrevMottakerValgDto> mottakere,
    boolean støtterFritekst,
    boolean støtterTittelOgFritekst
) {
}
