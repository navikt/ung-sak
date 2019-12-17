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

    private TestScenarioBuilder(FagsakYtelseType ytelseType) {
        super(ytelseType);
        settDefaultSøknad();
    }

    private TestScenarioBuilder(FagsakYtelseType ytelseType, AktørId aktørId) {
        super(ytelseType, aktørId);
        settDefaultSøknad();
    }

    private TestScenarioBuilder(FagsakYtelseType ytelseType, NavBruker navBruker) {
        super(ytelseType, navBruker);
        settDefaultSøknad();
    }

    private void settDefaultSøknad() {
            medSøknad()
                .medRelasjonsRolleType(RelasjonsRolleType.MORA)
                .medSøknadsdato(FPDateUtil.iDag());
    }

    public static TestScenarioBuilder builderMedSøknad() {
        return new TestScenarioBuilder(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
    }

    public static TestScenarioBuilder builderUtenSøknad() {
        return builderUtenSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
    }

    public static TestScenarioBuilder builderUtenSøknad(FagsakYtelseType ytelseType) {
        var scenario = new TestScenarioBuilder(ytelseType);
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderUtenSøknad(AktørId aktørId) {
        return builderUtenSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, aktørId);
    }
    
    public static TestScenarioBuilder builderUtenSøknad(FagsakYtelseType ytelseType, AktørId aktørId) {
        var scenario = new TestScenarioBuilder(ytelseType, aktørId);
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderMedSøknad(AktørId aktørId) {
        return builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, aktørId);
    }

    public static TestScenarioBuilder builderMedSøknad(FagsakYtelseType ytelseType, AktørId aktørId) {
        return new TestScenarioBuilder(ytelseType, aktørId);
    }

    public static TestScenarioBuilder builderMedSøknad(NavBruker navBruker) {
        return builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, navBruker);
    }

    public static TestScenarioBuilder builderMedSøknad(FagsakYtelseType fagsakYtelseType) {
        return new TestScenarioBuilder(fagsakYtelseType);
    }

    public static TestScenarioBuilder builderMedSøknad(FagsakYtelseType fagsakYtelseType, NavBruker navBruker) {
        return new TestScenarioBuilder(fagsakYtelseType, navBruker);
    }
}
