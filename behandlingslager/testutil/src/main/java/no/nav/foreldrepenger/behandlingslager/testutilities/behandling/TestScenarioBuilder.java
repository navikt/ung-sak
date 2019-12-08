package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Default test scenario builder.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public class TestScenarioBuilder extends AbstractTestScenario<TestScenarioBuilder> {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.FORELDREPENGER;

    private TestScenarioBuilder() {
        super(YTELSE_TYPE);
        settDefaultSøknad();

    }

    private TestScenarioBuilder(AktørId aktørId) {
        super(YTELSE_TYPE, aktørId);
        settDefaultSøknad();
    }

    private TestScenarioBuilder(NavBruker navBruker) {
        super(YTELSE_TYPE, navBruker);
        settDefaultSøknad();
    }

    private void settDefaultSøknad() {
            medSøknad()
                .medRelasjonsRolleType(RelasjonsRolleType.MORA)
                .medSøknadsdato(FPDateUtil.iDag());
    }

    public static TestScenarioBuilder builderMedSøknad() {
        return new TestScenarioBuilder();
    }

    public static TestScenarioBuilder builderUtenSøknad() {
        var scenario = new TestScenarioBuilder();
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderUtenSøknad(AktørId aktørId) {
        var scenario = new TestScenarioBuilder(aktørId);
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderMedSøknad(AktørId aktørId) {
        return new TestScenarioBuilder(aktørId);
    }

    public static TestScenarioBuilder builderMedSøknad(NavBruker navBruker) {
        return new TestScenarioBuilder(navBruker);
    }

}
