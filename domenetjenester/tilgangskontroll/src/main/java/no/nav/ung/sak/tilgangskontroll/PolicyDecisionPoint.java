package no.nav.ung.sak.tilgangskontroll;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.ung.sak.tilgangskontroll.tilganger.IkkeTilgangÅrsak;
import no.nav.ung.sak.tilgangskontroll.tilganger.AnsattTilgangerTjeneste;
import no.nav.ung.sak.tilgangskontroll.tilganger.TilgangTilOperasjonTjeneste;
import no.nav.ung.sak.tilgangskontroll.tilganger.TilgangTilPersonTjeneste;
import no.nav.ung.sak.tilgangskontroll.tilganger.TilgangerBruker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

@ApplicationScoped
public class PolicyDecisionPoint {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDecisionPoint.class);

    private AnsattTilgangerTjeneste ansattTilgangerTjeneste;
    private TilgangTilPersonTjeneste tilgangTilPersonTjeneste;
    private TilgangTilOperasjonTjeneste tilgangTilOperasjonTjeneste;

    PolicyDecisionPoint() {
        //for CDI proxy
    }

    @Inject
    public PolicyDecisionPoint(AnsattTilgangerTjeneste ansattTilgangerTjeneste, TilgangTilPersonTjeneste tilgangTilPersonTjeneste, TilgangTilOperasjonTjeneste tilgangTilOperasjonTjeneste) {
        this.ansattTilgangerTjeneste = ansattTilgangerTjeneste;
        this.tilgangTilPersonTjeneste = tilgangTilPersonTjeneste;
        this.tilgangTilOperasjonTjeneste = tilgangTilOperasjonTjeneste;
    }

    @WithSpan
    public Tilgangsbeslutning vurderTilgangForInnloggetBruker(PdpRequest pdpRequest) {
        TilgangsbeslutningInput input = new TilgangsbeslutningInput(pdpRequest);
        boolean harTilgang = vurderTilgangForInnloggetBruker(input);
        return new Tilgangsbeslutning(harTilgang, Set.of(harTilgang ? Decision.Permit : Decision.Deny), pdpRequest);
    }

    boolean vurderTilgangForInnloggetBruker(TilgangsbeslutningInput input) {
        TilgangerBruker tilganger = ansattTilgangerTjeneste.tilgangerForInnloggetBruker();
        Set<IkkeTilgangÅrsak> ikkeTilgangÅrsaker = sjekkTilganger(tilganger, input);
        return ikkeTilgangÅrsaker.isEmpty();
    }

    private Set<IkkeTilgangÅrsak> sjekkTilganger(TilgangerBruker tilganger, TilgangsbeslutningInput input) {
        Set<IkkeTilgangÅrsak> valideringsfeil = validerInput(input);
        if (!valideringsfeil.isEmpty()) {
            return valideringsfeil;
        }

        Set<IkkeTilgangÅrsak> ikkeTilgangÅrsaker = EnumSet.noneOf(IkkeTilgangÅrsak.class);
        ikkeTilgangÅrsaker.addAll(tilgangTilPersonTjeneste.sjekkTilgangTilPersoner(tilganger, input.getAktørIder(), input.getPersonIdenter()));
        ikkeTilgangÅrsaker.addAll(tilgangTilOperasjonTjeneste.sjekkTilgangTilOperasjon(tilganger, input.getOperasjon(), input.getSaksinformasjon()));
        return ikkeTilgangÅrsaker;
    }

    private static Set<IkkeTilgangÅrsak> validerInput(TilgangsbeslutningInput input) {
        if (input.getOperasjon().getResource() == TilgangsbeslutningInput.ResourceType.FAGSAK && input.getAktørIder().isEmpty() && input.getPersonIdenter().isEmpty()) {
            logger.warn("Har fagsak-resource, men fikk ikke noen personer. Gir ikke tilgang.");
            return Set.of(IkkeTilgangÅrsak.TEKNISK_FEIL);
        }
        if (input.getOperasjon().getResource() == TilgangsbeslutningInput.ResourceType.APPLIKASJON && !(input.getAktørIder().isEmpty() && input.getPersonIdenter().isEmpty())) {
            logger.warn("Har applikasjon-resource, men fikk noen personer. Gir ikke tilgang. Undersøk om det er benyttet riktig resource");
            return Set.of(IkkeTilgangÅrsak.TEKNISK_FEIL);
        }
        return Set.of();
    }


}
