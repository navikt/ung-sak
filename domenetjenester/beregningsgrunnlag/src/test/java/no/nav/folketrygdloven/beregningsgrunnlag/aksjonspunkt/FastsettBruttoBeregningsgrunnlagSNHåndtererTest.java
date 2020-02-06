package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNDto;

public class FastsettBruttoBeregningsgrunnlagSNHåndtererTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repositoryRule.getEntityManager());

    private static final int BRUTTO_BG = 200000;
    private FastsettBruttoBeregningsgrunnlagSNHåndterer fastsettBruttoBeregningsgrunnlagSNHåndterer;
    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setup() {
        fastsettBruttoBeregningsgrunnlagSNHåndterer = new FastsettBruttoBeregningsgrunnlagSNHåndterer(beregningsgrunnlagRepository);
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi() {
        //Arrange
        int antallPerioder = 1;
        lagBehandlingMedBeregningsgrunnlag(antallPerioder);

        //Dto
        var fastsettBGDto = new FastsettBruttoBeregningsgrunnlagSNDto("begrunnelse", BRUTTO_BG);

        // Act
        fastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(behandlingReferanse.getId(), fastsettBGDto);

        //Assert
        assertBeregningsgrunnlag(antallPerioder);
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_for_fleire_perioder() {
        //Arrange
        int antallPerioder = 3;
        lagBehandlingMedBeregningsgrunnlag(antallPerioder);

        //Dto
        var fastsettBGDto = new FastsettBruttoBeregningsgrunnlagSNDto("begrunnelse", BRUTTO_BG);

        // Act
        fastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(behandlingReferanse.getId(), fastsettBGDto);

        //Assert
        assertBeregningsgrunnlag(antallPerioder);
    }

    private void assertBeregningsgrunnlag(int antallPerioder) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlag).as("beregningsgrunnlag").hasValueSatisfying(bg -> {
            List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = bg.getBeregningsgrunnlagPerioder();
            assertThat(beregningsgrunnlagPerioder).hasSize(antallPerioder);
            beregningsgrunnlagPerioder.forEach(beregningsgrunnlagPeriode -> {
                List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
                assertThat(beregningsgrunnlagPrStatusOgAndelList).hasSize(1);
                assertThat(beregningsgrunnlagPrStatusOgAndelList.get(0).getBruttoPrÅr().doubleValue()).isEqualTo(BRUTTO_BG);
            });
        });
    }

    private void buildBgPrStatusOgAndel(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(beregningsgrunnlagPeriode);
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                                                                          LocalDate fom,
                                                                                                                                          LocalDate tom) {
        return no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

    private void lagBehandlingMedBeregningsgrunnlag(int antallPerioder) {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .build();

        for (int i = 0; i < antallPerioder; i++) {
            LocalDate fom = LocalDate.now().minusDays(20).plusDays(i*5).plusDays(i==0 ? 0 : 1);
            no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag,
                fom, fom.plusDays(5));
            buildBgPrStatusOgAndel(bgPeriode);
        }
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }
}
