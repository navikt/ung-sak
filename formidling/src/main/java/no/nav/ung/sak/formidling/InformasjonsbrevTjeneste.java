package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;

import java.util.Collections;
import java.util.List;

@Dependent
public class InformasjonsbrevTjeneste {

    @Inject
    public InformasjonsbrevTjeneste() {
    }

    public List<InformasjonsbrevValgDto> informasjonsbrevValg(Long behandlingId) {
        return Collections.emptyList();
    }
}
