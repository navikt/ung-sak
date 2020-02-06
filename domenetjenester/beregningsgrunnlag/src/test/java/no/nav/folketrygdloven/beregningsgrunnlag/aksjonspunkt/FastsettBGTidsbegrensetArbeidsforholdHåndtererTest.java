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
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsatteAndelerTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsattePerioderTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class FastsettBGTidsbegrensetArbeidsforholdHåndtererTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();

    private FastsettBGTidsbegrensetArbeidsforholdHåndterer fastsettBGTidsbegrensetArbeidsforholdHåndterer;
    private BehandlingReferanse behandlingReferanse;
    private List<FastsattePerioderTidsbegrensetDto> fastsatteInnteker;
    private final LocalDate FØRSTE_PERIODE_FOM = LocalDate.now().minusDays(100);
    private final LocalDate FØRSTE_PERIODE_TOM = LocalDate.now().minusDays(50);
    private final LocalDate ANDRE_PERIODE_FOM = LocalDate.now().minusDays(49);
    private final LocalDate ANDRE_PERIODE_TOM = LocalDate.now();
    private final Long FØRSTE_ANDELSNR = 1L;
    private final Long ANDRE_ANDELSNR = 2L;
    private final Integer FØRSTE_PERIODE_FØRSTE_ANDEL_INNTEKT = 100000;
    private final Integer FØRSTE_PERIODE_ANDRE_ANDEL_INNTEKT = 200000;
    private final Integer ANDRE_PERIODE_FØRSTE_ANDEL_INNTEKT = 300000;
    private final Integer ANDRE_PERIODE_ANDRE_ANDEL_INNTEKT = 400000;
    private VirksomhetEntitet virksomhet1;
    private VirksomhetEntitet virksomhet2;


    @Before
    public void setup() {
        fastsettBGTidsbegrensetArbeidsforholdHåndterer = new FastsettBGTidsbegrensetArbeidsforholdHåndterer(beregningsgrunnlagRepository);
        fastsatteInnteker = lagFastsatteAndelerListe();
        virksomhet1 = new VirksomhetEntitet.Builder()
                .medOrgnr("123")
                .medNavn("VirksomhetNavn1")
                .oppdatertOpplysningerNå()
                .build();
        repositoryProvider.getVirksomhetRepository().lagre(virksomhet1);
        virksomhet2 = new VirksomhetEntitet.Builder()
                .medOrgnr("456")
                .medNavn("VirksomhetNavn2")
                .oppdatertOpplysningerNå()
                .build();
        repositoryProvider.getVirksomhetRepository().lagre(virksomhet2);
        repositoryRule.getEntityManager().flush();
    }

    private List<FastsattePerioderTidsbegrensetDto> lagFastsatteAndelerListe() {
        FastsatteAndelerTidsbegrensetDto andelEnPeriodeEn = new FastsatteAndelerTidsbegrensetDto(FØRSTE_ANDELSNR, FØRSTE_PERIODE_FØRSTE_ANDEL_INNTEKT);
        FastsatteAndelerTidsbegrensetDto andelToPeriodeEn = new FastsatteAndelerTidsbegrensetDto(ANDRE_ANDELSNR, FØRSTE_PERIODE_ANDRE_ANDEL_INNTEKT);

        FastsattePerioderTidsbegrensetDto førstePeriode = new FastsattePerioderTidsbegrensetDto(
            FØRSTE_PERIODE_FOM,
            FØRSTE_PERIODE_TOM,
            List.of(andelEnPeriodeEn, andelToPeriodeEn)
        );

        FastsatteAndelerTidsbegrensetDto andelEnPeriodeTo = new FastsatteAndelerTidsbegrensetDto(FØRSTE_ANDELSNR, ANDRE_PERIODE_FØRSTE_ANDEL_INNTEKT);
        FastsatteAndelerTidsbegrensetDto andelToPeriodeTo = new FastsatteAndelerTidsbegrensetDto(ANDRE_ANDELSNR, ANDRE_PERIODE_ANDRE_ANDEL_INNTEKT);

        FastsattePerioderTidsbegrensetDto andrePeriode = new FastsattePerioderTidsbegrensetDto(
            ANDRE_PERIODE_FOM,
            ANDRE_PERIODE_TOM,
            List.of(andelEnPeriodeTo, andelToPeriodeTo)
        );

        return List.of(førstePeriode, andrePeriode);
    }


    @Test
    public void skal_sette_korrekt_overstyrtSum_på_korrekt_periode_og_korrekt_andel() {
        //Arrange
        lagBehandlingMedBeregningsgrunnlag();

        //Dto
        var dto = new FastsettBGTidsbegrensetArbeidsforholdDto("begrunnelse", fastsatteInnteker);

        // Act
        fastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());

        assertThat(beregningsgrunnlag.isPresent()).isTrue();
        BeregningsgrunnlagPeriode førstePeriode = beregningsgrunnlag.get().getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPeriode andrePeriode = beregningsgrunnlag.get().getBeregningsgrunnlagPerioder().get(1);
        assertThat(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getOverstyrtPrÅr()).isEqualTo(BigDecimal.valueOf(FØRSTE_PERIODE_FØRSTE_ANDEL_INNTEKT));
        assertThat(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(1).getOverstyrtPrÅr()).isEqualTo(BigDecimal.valueOf(FØRSTE_PERIODE_ANDRE_ANDEL_INNTEKT));
        assertThat(andrePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0).getOverstyrtPrÅr()).isEqualTo(BigDecimal.valueOf(ANDRE_PERIODE_FØRSTE_ANDEL_INNTEKT));
        assertThat(andrePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(1).getOverstyrtPrÅr()).isEqualTo(BigDecimal.valueOf(ANDRE_PERIODE_ANDRE_ANDEL_INNTEKT));
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Virksomhet virksomhet) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2))
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhet.getOrgnr()));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(beregningsgrunnlag);
    }

    private void lagBehandlingMedBeregningsgrunnlag() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        BeregningsgrunnlagEntitet beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .build();


        BeregningsgrunnlagPeriode førstePeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag,
            FØRSTE_PERIODE_FOM, FØRSTE_PERIODE_TOM);
        buildBgPrStatusOgAndel(førstePeriode, virksomhet1);
        buildBgPrStatusOgAndel(førstePeriode, virksomhet2);

        BeregningsgrunnlagPeriode andrePeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag,
            ANDRE_PERIODE_FOM, ANDRE_PERIODE_TOM);
        buildBgPrStatusOgAndel(andrePeriode, virksomhet1);
        buildBgPrStatusOgAndel(andrePeriode, virksomhet2);

        behandlingReferanse = scenario.lagre(repositoryProvider);
    }
}
