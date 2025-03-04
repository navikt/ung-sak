package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonTilgangskontrollInputDto;

import java.util.Set;

@Dependent
@Alternative
@Priority(1)
public class AppPdpKlient implements PdpKlient {

    private final SifAbacPdpRestKlient sifAbacPdpRestKlient;

    @Inject
    public AppPdpKlient(SifAbacPdpRestKlient sifAbacPdpRestKlient) {
        this.sifAbacPdpRestKlient = sifAbacPdpRestKlient;
    }

    @Override
    public Tilgangsbeslutning forespørTilgang(PdpRequest pdpRequest) {
        SaksinformasjonTilgangskontrollInputDto tilgangskontrollInput = PdpRequestMapper.map(pdpRequest);
        Decision decision = sifAbacPdpRestKlient.sjekkTilgangForInnloggetBruker(tilgangskontrollInput);
        return new Tilgangsbeslutning(decision == Decision.Permit, Set.of(), pdpRequest);
    }

}

