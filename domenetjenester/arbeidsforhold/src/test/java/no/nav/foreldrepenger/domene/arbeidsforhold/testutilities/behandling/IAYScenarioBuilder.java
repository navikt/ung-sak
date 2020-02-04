package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

/**
 * Default test scenario builder for Mor søker Engangsstønad. Kan opprettes for fødsel eller adopsjon og brukes til å
 * opprette standard scenarioer.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class IAYScenarioBuilder extends AbstractIAYTestScenario<IAYScenarioBuilder>{

    private IAYScenarioBuilder(FagsakYtelseType ytelseType) {
        super(ytelseType);
    }

    public static IAYScenarioBuilder nyttScenario(FagsakYtelseType ytelseType) {
        return new IAYScenarioBuilder(ytelseType);
    }
    
    
}
