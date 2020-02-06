package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.FaktaOmBeregningTilfellerOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsatteVerdierDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.RedigerbarAndelDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;


@RunWith(CdiRunner.class)
public class BeregningFaktaOgOverstyringHåndtererTest {

    private static final LocalDate STP = LocalDate.of(2019, 1, 1);

    @Inject
    private FaktaOmBeregningTilfellerOppdaterer faktaOmBeregningTilfellerOppdaterer;
    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repositoryRule.getEntityManager());

    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    public TestScenarioBuilder scenario;
    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setup() {
        this.beregningFaktaOgOverstyringHåndterer = new BeregningFaktaOgOverstyringHåndterer(faktaOmBeregningTilfellerOppdaterer, beregningsgrunnlagRepository);
        this.scenario = TestScenarioBuilder.nyttScenario();
        this.behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_sette_inntekt_for_en_andel_i_en_periode() {
        // Arrange
        Long andelsnr = 1L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(andelsnr, List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(STP, null)));
        int fastsattBeløp = 10000;
        OverstyrBeregningsgrunnlagDto overstyrDto = new OverstyrBeregningsgrunnlagDto(lagFastsattAndeler(andelsnr, fastsattBeløp), "begrunnelsen");
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(behandlingReferanse, overstyrDto);

        // Assert
        Optional<BeregningsgrunnlagEntitet> lagretBg = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(lagretBg).isPresent();
        assertThat(lagretBg.get().isOverstyrt()).isTrue();
        List<BeregningsgrunnlagPeriode> perioder = lagretBg.get().getBeregningsgrunnlagPerioder();
        assertThat(perioder.size()).isEqualTo(1);
        BeregningsgrunnlagPeriode p1 = perioder.get(0);
        assertThat(p1.getBeregningsgrunnlagPeriodeFom()).isEqualTo(STP);
        validerAndeler(fastsattBeløp, p1);
    }

    @Test
    public void skal_sette_inntekt_for_en_andel_i_to_perioder() {
        // Arrange
        Long andelsnr = 1L;
        LocalDate tilOgMed = STP.plusMonths(1).minusDays(1);
        List<ÅpenDatoIntervallEntitet> periodeList = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(STP, tilOgMed),
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(tilOgMed.plusDays(1), null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag(andelsnr,
            periodeList);
        int fastsattBeløp1 = 10000;
        OverstyrBeregningsgrunnlagDto overstyrDto = new OverstyrBeregningsgrunnlagDto(lagFastsattAndeler(andelsnr, fastsattBeløp1), "begrunnelsen");
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(behandlingReferanse, overstyrDto);

        // Assert
        Optional<BeregningsgrunnlagEntitet> lagretBg = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(lagretBg).isPresent();
        assertThat(lagretBg.get().isOverstyrt()).isTrue();
        List<BeregningsgrunnlagPeriode> perioder = lagretBg.get().getBeregningsgrunnlagPerioder();
        assertThat(perioder.size()).isEqualTo(2);
        BeregningsgrunnlagPeriode p1 = perioder.get(0);
        assertThat(p1.getBeregningsgrunnlagPeriodeFom()).isEqualTo(STP);
        validerAndeler(fastsattBeløp1, p1);
        BeregningsgrunnlagPeriode p2 = perioder.get(1);
        assertThat(p2.getBeregningsgrunnlagPeriodeFom()).isEqualTo(tilOgMed.plusDays(1));
        validerAndeler(fastsattBeløp1, p2);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(Long andelsnr, List<ÅpenDatoIntervallEntitet> perioder) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(STP)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        perioder.forEach(p -> {
            BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(p.getFomDato(), p.getTomDato())
                .build(beregningsgrunnlag);
            BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(andelsnr)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(AktørId.dummy())))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build(periode);
        });
        return beregningsgrunnlag;
    }

    private void validerAndeler(int fastsattBeløp, BeregningsgrunnlagPeriode p1) {
        assertThat(p1.getBeregningsgrunnlagPrStatusOgAndelList().size()).isEqualTo(1);
        assertThat(p1.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getBeregnetPrÅr().intValue()).isEqualTo(fastsattBeløp * 12);
        assertThat(p1.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getFastsattAvSaksbehandler()).isTrue();
        assertThat(p1.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
    }

    private List<FastsettBeregningsgrunnlagAndelDto> lagFastsattAndeler(Long andelsnr, int fastsattBeløp1) {
        RedigerbarAndelDto andelsInfo = new RedigerbarAndelDto(false, andelsnr, false, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
        FastsatteVerdierDto fastsatteVerdier1 = new FastsatteVerdierDto(null, fastsattBeløp1, null, null);
        FastsettBeregningsgrunnlagAndelDto andelDto1 = new FastsettBeregningsgrunnlagAndelDto(andelsInfo, fastsatteVerdier1, Inntektskategori.ARBEIDSTAKER, null,null);
        return List.of(andelDto1);
    }

}
