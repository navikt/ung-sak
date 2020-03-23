package no.nav.k9.sak.behandlingskontroll.testutilities;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

/**
 * Default test scenario builder for Behandlingskontroll enhetstester.
 */
public class TestScenario extends AbstractTestScenario<TestScenario> {

    private TestScenario(FagsakYtelseType ytelseType) {
        super(ytelseType);
    }
    
    public static TestScenario forYtelseType(FagsakYtelseType ytelseType) {
        return new TestScenario(ytelseType);
    }

    public static TestScenario forForeldrepenger() {
        return forYtelseType(FagsakYtelseType.FORELDREPENGER);
    }

}
