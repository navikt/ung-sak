package no.nav.ung.sak.test.util.behandling;

import java.time.LocalDate;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.typer.AktørId;

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

    public static final FagsakYtelseType DEFAULT_TEST_YTELSE = FagsakYtelseType.UNGDOMSYTELSE;

    private TestScenarioBuilder(FagsakYtelseType ytelseType) {
        super(ytelseType);
        settDefaultSøknad();
    }

    private TestScenarioBuilder(FagsakYtelseType ytelseType, AktørId aktørId) {
        super(ytelseType, aktørId);
        settDefaultSøknad();
    }

    private void settDefaultSøknad() {
            medSøknad()
                .medRelasjonsRolleType(RelasjonsRolleType.MORA)
                .medSøknadsdato(LocalDate.now());
    }

    public static TestScenarioBuilder builderMedSøknad() {
        return new TestScenarioBuilder(DEFAULT_TEST_YTELSE);
    }

    public static TestScenarioBuilder builderUtenSøknad() {
        return builderUtenSøknad(DEFAULT_TEST_YTELSE);
    }

    public static TestScenarioBuilder builderUtenSøknad(FagsakYtelseType ytelseType) {
        var scenario = new TestScenarioBuilder(ytelseType);
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderUtenSøknad(AktørId aktørId) {
        return builderUtenSøknad(DEFAULT_TEST_YTELSE, aktørId);
    }

    public static TestScenarioBuilder builderUtenSøknad(FagsakYtelseType ytelseType, AktørId aktørId) {
        var scenario = new TestScenarioBuilder(ytelseType, aktørId);
        scenario.utenSøknad();
        return scenario;
    }

    public static TestScenarioBuilder builderMedSøknad(AktørId aktørId) {
        return builderMedSøknad(DEFAULT_TEST_YTELSE, aktørId);
    }

    public static TestScenarioBuilder builderMedSøknad(FagsakYtelseType ytelseType, AktørId aktørId) {
        return new TestScenarioBuilder(ytelseType, aktørId);
    }

    public static TestScenarioBuilder builderMedSøknad(FagsakYtelseType fagsakYtelseType) {
        return new TestScenarioBuilder(fagsakYtelseType);
    }

}
