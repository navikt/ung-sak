package no.nav.ung.sak.behandlingskontroll.testutilities;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

/**
 * Default test scenario builder for Behandlingskontroll enhetstester.
 */
public class TestScenario extends AbstractTestScenario<TestScenario> {

    // bruker denne for dummy testing (så får teste bibliotek/apier uten å bind opp til konfigurasjon gyldig for ung-sak)
    public static final FagsakYtelseType DUMMY_YTELSE_TYPE = FagsakYtelseType.SVANGERSKAPSPENGER;

    private TestScenario(FagsakYtelseType ytelseType) {
        super(ytelseType);
    }

    public static TestScenario forYtelseType(FagsakYtelseType ytelseType) {
        return new TestScenario(ytelseType);
    }

    public static TestScenario dummyScenario() {
        return forYtelseType(DUMMY_YTELSE_TYPE);
    }

}
