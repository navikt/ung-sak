package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettEtterlønnSluttpakkeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Beløp;

public class FastsettEtterlønnSluttpakkeOppdatererTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019,1,1);
    private static final Beløp GRUNNBELØP = new Beløp(BigDecimal.valueOf(85000));
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private FastsettEtterlønnSluttpakkeOppdaterer fastsettEtterlønnSluttpakkeOppdaterer;
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.fra(new VirksomhetEntitet.Builder().medOrgnr("490830958").build());

    @Before
    public void setup() {
        beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        fastsettEtterlønnSluttpakkeOppdaterer = new FastsettEtterlønnSluttpakkeOppdaterer();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        beregningsgrunnlag = lagBeregningsgrunnlag();
    }

    @Test
    public void skalTesteAtOppdatererSetterKorrektInntektPåSøkerensEtterlønnSluttpakkeAndel() {
        // Arrange
        FastsettEtterlønnSluttpakkeDto fastsettDto = new FastsettEtterlønnSluttpakkeDto(10000);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(singletonList(FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE));
        dto.setFastsettEtterlønnSluttpakke(fastsettDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        fastsettEtterlønnSluttpakkeOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());

        // Assert
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttBg.getBeregningsgrunnlagPerioder();
        assertThat(bgPerioder).hasSize(1);
        assertThat(bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        BeregningsgrunnlagPrStatusOgAndel andel = bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> a.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke forventet andel etter å ha kjørt oppdaterer"));
        assertThat(andel.getBeregnetPrÅr().compareTo(BigDecimal.valueOf(120000)) == 0).isTrue();
        assertThat(andel.getFastsattAvSaksbehandler()).isTrue();
        assertThat(andel.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .build();

        BeregningsgrunnlagPeriode periode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag,
            SKJÆRINGSTIDSPUNKT, null);
        buildBgPrStatusOgAndel(periode);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);
        return beregningsgrunnlag;
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2))
            .build(beregningsgrunnlagPeriode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ARBEID)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2))
            .build(beregningsgrunnlagPeriode);

    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

}
