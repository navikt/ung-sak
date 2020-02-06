package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderEtterlønnSluttpakkeDto;
import no.nav.k9.sak.typer.Beløp;

public class VurderEtterlønnSluttpakkeOppdatererTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019,1,1);
    private static final Beløp GRUNNBELØP = new Beløp(BigDecimal.valueOf(85000));
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private VurderEtterlønnSluttpakkeOppdaterer vurderEtterlønnSluttpakkeOppdaterer = new VurderEtterlønnSluttpakkeOppdaterer();

    private BehandlingReferanse behandlingReferanse;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Before
    public void setup() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        beregningsgrunnlag = lagBeregningsgrunnlag();
    }

    @Test
    public void skalTesteAtOppdatererSetterInntekt0DersomBrukerIkkeHarEtterlønnSluttpakke() {
        // Arrange
        VurderEtterlønnSluttpakkeDto vurderDto = new VurderEtterlønnSluttpakkeDto(false);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE));
        dto.setVurderEtterlønnSluttpakke(vurderDto);

        // Act
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlag.dypKopi();
        vurderEtterlønnSluttpakkeOppdaterer.oppdater(dto, behandlingReferanse, nyttBg, Optional.empty());

        // Assert
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttBg.getBeregningsgrunnlagPerioder();
        assertThat(bgPerioder).hasSize(1);
        assertThat(bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(andel.getBeregnetPrÅr().compareTo(BigDecimal.ZERO) == 0).isTrue();
        assertThat(andel.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE);
    }

    @Test
    public void skalTesteAtOppdatererIkkeSetterInntektDersomBrukerHarEtterlønnSluttpakke() {
        // Arrange
        VurderEtterlønnSluttpakkeDto vurderDto = new VurderEtterlønnSluttpakkeDto(true);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE));
        dto.setVurderEtterlønnSluttpakke(vurderDto);

        // Act
        vurderEtterlønnSluttpakkeOppdaterer.oppdater(dto, behandlingReferanse, beregningsgrunnlag, Optional.empty());

        // Assert
        BeregningsgrunnlagEntitet nyttBg = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttBg.getBeregningsgrunnlagPerioder();
        assertThat(bgPerioder).hasSize(1);
        assertThat(bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = bgPerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(andel.getBeregnetPrÅr()).isNull();
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
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

}
