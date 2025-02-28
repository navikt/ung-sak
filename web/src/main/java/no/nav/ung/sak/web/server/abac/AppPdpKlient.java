package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonTilgangskontrollInputDto;
import no.nav.ung.sak.tilgangskontroll.PolicyDecisionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Dependent
@Alternative
@Priority(1)
public class AppPdpKlient implements PdpKlient {

    private static final Logger LOG = LoggerFactory.getLogger(AppPdpKlient.class);

    private final PolicyDecisionPoint pdp;
    private final SifAbacPdpRestKlient sifAbacPdpRestKlient;

    @Inject
    public AppPdpKlient(PolicyDecisionPoint pdp, SifAbacPdpRestKlient sifAbacPdpRestKlient) {
        this.pdp = pdp;
        this.sifAbacPdpRestKlient = sifAbacPdpRestKlient;
    }

    @Override
    public Tilgangsbeslutning forespørTilgang(PdpRequest pdpRequest) {
        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(pdpRequest);

        try {
            Tilgangsbeslutning eksterntSvar = forespørTilgangEksternt(pdpRequest);
            if (eksterntSvar.fikkTilgang() != tilgangsbeslutning.fikkTilgang()) {
                LOG.warn("Ulikt svar på tilgang. Intern {} og ekstern {}", tilgangsbeslutning.fikkTilgang(), eksterntSvar.fikkTilgang());
                if (Environment.current().isDev()) {
                    LOG.info("Forespørsel som gav ulikt svar var: {}", pdpRequest);
                }
            }
        } catch (Exception e) {
            LOG.warn("Feil med ekstern tilgangskontroll", e);
        }

        return tilgangsbeslutning;
    }

    public Tilgangsbeslutning forespørTilgangEksternt(PdpRequest pdpRequest) {
        SaksinformasjonTilgangskontrollInputDto tilgangskontrollInput = PdpRequestMapper.map(pdpRequest);
        Decision decision = sifAbacPdpRestKlient.sjekkTilgangForInnloggetBruker(tilgangskontrollInput);
        return new Tilgangsbeslutning(decision == Decision.Permit, Set.of(), pdpRequest);
    }

}

