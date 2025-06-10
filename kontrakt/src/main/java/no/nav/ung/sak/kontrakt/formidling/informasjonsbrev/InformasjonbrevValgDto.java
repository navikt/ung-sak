package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;

import java.util.List;

public record InformasjonbrevValgDto(
    InformasjonsbrevMalType malType,
    List<InformasjonsbrevMottakerDto> mottakere,
    boolean støtterFritekst,
    boolean støtterTittelOgFritekst,
    boolean støtterTredjepartsMottaker
) {
}
