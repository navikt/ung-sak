package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.typer.Beløp;
import no.nav.vedtak.exception.TekniskException;

public class MatchBeregningsgrunnlagTjenesteTest {


    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);
    private static final Arbeidsgiver arbeidsgiverEn = Arbeidsgiver.virksomhet("973152351");

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private MatchBeregningsgrunnlagTjeneste matchBeregningsgrunnlagTjeneste = new MatchBeregningsgrunnlagTjeneste(repositoryProvider.getBeregningsgrunnlagRepository());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    public TestScenarioBuilder scenario;
    public BehandlingReferanse behandlingReferanse;

    @Before
    public void setUp() {
        this.scenario = TestScenarioBuilder.nyttScenario();
        this.behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private BeregningsgrunnlagPrStatusOgAndel lagreBgForBehandling(InternArbeidsforholdRef arbId, Long andelsnr, BehandlingReferanse revurdering, BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                   Inntektskategori arbeidstaker, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = lagArbeidstakerAndelNr1(arbId, andelsnr, periode, arbeidstaker);
        beregningsgrunnlagRepository.lagre(revurdering.getId(), beregningsgrunnlag, tilstand);
        return andel;
    }

    @Test
    public void skal_finne_andel_i_beregningsgrunnlag_fra_kofakber_ut() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        BeregningsgrunnlagPrStatusOgAndel andelKofakBerUt = byggBgOriginalBehandlingKofakberUt(arbId);
        BeregningsgrunnlagPrStatusOgAndel andel = byggBgOriginalBehandling(arbId);

        // Act
        Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndel = matchBeregningsgrunnlagTjeneste.hentLikAndelIBeregningsgrunnlag(andel,
            andelKofakBerUt.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag());

        // Assert
        assertThat(korrektAndel.get().equals(andelKofakBerUt)).isTrue();
    }

    @Test
    public void skal_finne_andel_i_gjeldende_beregningsgrunnlag() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        BeregningsgrunnlagPrStatusOgAndel andelFraGjeldende = byggBgOriginalBehandlingFastsatt(arbId);
        BeregningsgrunnlagPrStatusOgAndel andel = byggBgOriginalBehandlingMedGjeldende(arbId, andelFraGjeldende.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag());

        // Act
        Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndel = matchBeregningsgrunnlagTjeneste.hentLikAndelIBeregningsgrunnlag(andel, andelFraGjeldende.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag());

        // Assert
        assertThat(korrektAndel.get().equals(andelFraGjeldende)).isTrue();
    }

    private BeregningsgrunnlagPrStatusOgAndel byggBgOriginalBehandling(InternArbeidsforholdRef arbId) {
        Long andelsnr = 1L;
        return lagreBgForBehandling(arbId, andelsnr, behandlingReferanse, lagBeregningsgrunnlag(), Inntektskategori.ARBEIDSTAKER, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private BeregningsgrunnlagPrStatusOgAndel byggBgOriginalBehandlingMedGjeldende(InternArbeidsforholdRef arbId, BeregningsgrunnlagEntitet gjeldende) {
        Long andelsnr = 1L;
        return lagreBgForBehandling(arbId, andelsnr, behandlingReferanse, gjeldende, Inntektskategori.ARBEIDSTAKER, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private BeregningsgrunnlagPrStatusOgAndel lagArbeidstakerAndelNr1(InternArbeidsforholdRef arbId, Long andelsnr, BeregningsgrunnlagPeriode periode, Inntektskategori inntektskategori) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAndelsnr(andelsnr)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiverEn).medArbeidsforholdRef(arbId))
                .medLagtTilAvSaksbehandler(false)
                .medInntektskategori(inntektskategori)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
    }

    private BeregningsgrunnlagPeriode lagPeriode(BeregningsgrunnlagEntitet bg) {
        return BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
                .build(bg);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.builder()
                .medGrunnbeløp(GRUNNBELØP)
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
    }

    private BeregningsgrunnlagPrStatusOgAndel byggBgOriginalBehandlingKofakberUt(InternArbeidsforholdRef arbId) {
        Long andelsnr = 1L;
        BeregningsgrunnlagPrStatusOgAndel andel = lagreBgForBehandling(arbId, andelsnr, behandlingReferanse, lagBeregningsgrunnlag(), Inntektskategori.ARBEIDSTAKER, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        return andel;
    }

    private BeregningsgrunnlagPrStatusOgAndel byggBgOriginalBehandlingFastsatt(InternArbeidsforholdRef arbId) {
        Long andelsnr = 1L;
        BeregningsgrunnlagPrStatusOgAndel andel = lagreBgForBehandling(arbId, andelsnr, behandlingReferanse, lagBeregningsgrunnlag(), Inntektskategori.ARBEIDSTAKER, BeregningsgrunnlagTilstand.FASTSATT);
        return andel;
    }


    @Test
    public void skal_matche_på_andelsnr() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nullRef();
        Long andelsnr = 1L;

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode =    lagPeriode(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(andelsnr)
            .medLagtTilAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.SJØMANN)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiverEn))
            .build(periode);

        // Act
        BeregningsgrunnlagPrStatusOgAndel korrektAndel = MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriode(periode, andelsnr, arbId);

        // Assert
        assertThat(korrektAndel).isEqualTo(andel);
    }

    @Test
    public void skal_matche_på_arbid_om_andelsnr_input_er_null() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiverEn).medArbeidsforholdRef(arbId))
            .medAndelsnr(andelsnr)
            .medLagtTilAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.SJØMANN)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        // Act
        BeregningsgrunnlagPrStatusOgAndel korrektAndel = MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriode(periode, null, arbId);

        // Assert
        assertThat(korrektAndel).isEqualTo(andel);
    }

    @Test(expected = TekniskException.class)
    public void skal_kaste_exception_om_andel_ikkje_eksisterer_i_grunnlag() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiverEn).medArbeidsforholdRef(arbId))
            .medAndelsnr(andelsnr)
            .medLagtTilAvSaksbehandler(true)
            .medInntektskategori(Inntektskategori.SJØMANN)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        // Act
        MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriode(periode, 2L, InternArbeidsforholdRef.nyRef());
    }
}
