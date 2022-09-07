package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;


import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.Pep;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;

/**
 * Brukes for å manuelt sjekke om kaller har tilgang til en sak, typisk ved rapportgenerering
 */
@ApplicationScoped
public class DriftLesetilgangVurderer {
    private Pep pep;
    private BrukerTokenProvider tokenProvider;

    public DriftLesetilgangVurderer() {}

    @Inject
    public DriftLesetilgangVurderer(Pep pep, BrukerTokenProvider tokenProvider) {
        this.pep = pep;
        this.tokenProvider = tokenProvider;
    }

    public boolean harTilgang(String saksnummer) {
        return harTilgang(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer(saksnummer)));
    }

    public boolean harTilgang(String saksnummer, String fnr, String aktørId) {
        AbacDataAttributter dataAttributter = AbacDataAttributter.opprett()
            .leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer(saksnummer));

        if (fnr != null) dataAttributter.leggTil(StandardAbacAttributtType.FNR, fnr);
        if (aktørId != null) dataAttributter.leggTil(StandardAbacAttributtType.AKTØR_ID, aktørId);

        return harTilgang(dataAttributter);
    }

    private boolean harTilgang(AbacDataAttributter dataAttributter) {
        final AbacAttributtSamling attributter = AbacAttributtSamling.medJwtToken(tokenProvider.getToken().getToken());
        attributter.setActionType(BeskyttetRessursActionAttributt.READ);
        attributter.setResource(DRIFT);

        // Package private:
        //attributter.setAction(restApiPath);
        attributter.leggTil(dataAttributter);

        final Tilgangsbeslutning beslutning = pep.vurderTilgang(attributter);
        return beslutning.fikkTilgang();
    }
}
