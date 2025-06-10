package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgResponseDto;

import java.util.Collections;

@Dependent
public class InformasjonsbrevTjeneste {

    @Inject
    public InformasjonsbrevTjeneste() {
    }

    public InformasjonsbrevValgResponseDto informasjonsbrevValg(Long behandlingId) {
        return new InformasjonsbrevValgResponseDto(Collections.emptyList());
    }
}
