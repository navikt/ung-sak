package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

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
    InformasjonsbrevMalType malType,
    List<InformasjonsbrevMottakerValgDto> mottakere,
    boolean støtterFritekst,
    boolean støtterTittelOgFritekst
) {
}
