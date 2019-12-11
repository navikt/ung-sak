package no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Default test scenario builder.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
public class TestScenarioBuilder extends AbstractTestScenario<TestScenarioBuilder> {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.FORELDREPENGER;

    private TestScenarioBuilder(RelasjonsRolleType relasjonRolle, NavBrukerKjønn kjønn) {
        super(YTELSE_TYPE, relasjonRolle, kjønn);

    }

    public static TestScenarioBuilder nyttScenario() {
        return new TestScenarioBuilder(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
    }

    public BeregningAktivitetAggregatEntitet.Builder medBeregningAktiviteter() {
        BeregningAktivitetScenario beregningAktivitetScenario = new BeregningAktivitetScenario();
        leggTilScenario(beregningAktivitetScenario);
        return beregningAktivitetScenario.getBeregningAktiviteterBuilder();
    }

    public BeregningsgrunnlagEntitet.Builder medBeregningsgrunnlag() {
        BeregningsgrunnlagScenario beregningsgrunnlagScenario = new BeregningsgrunnlagScenario();
        leggTilScenario(beregningsgrunnlagScenario);
        return beregningsgrunnlagScenario.getBeregningsgrunnlagBuilder();
    }

    public static TestScenarioBuilder nyttScenarioFar() {
        return new TestScenarioBuilder(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN);
    }


}
