package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBeregningsgrunnlagATFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.InntektPrAndelDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class FastsettBeregningsgrunnlagATFLHåndtererTest {
    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repositoryRule.getEntityManager());

    private BehandlingReferanse behandlingReferanse;
    private FastsettBeregningsgrunnlagATFLHåndterer oppdaterer;

    private final List<VirksomhetEntitet> virksomheter = new ArrayList<>();

    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int BRUTTO_PR_AR = 150000;
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;

    @Before
    public void setup() {
        virksomheter.add(new VirksomhetEntitet.Builder()
                .medOrgnr("991825827")
                .medNavn("AF0")
                .oppdatertOpplysningerNå()
                .build());
        virksomheter.add(new VirksomhetEntitet.Builder()
                .medOrgnr("974760673")
                .medNavn("AF1")
                .oppdatertOpplysningerNå()
                .build());
        virksomheter.forEach(v -> repositoryProvider.getVirksomhetRepository().lagre(v));
        oppdaterer = new FastsettBeregningsgrunnlagATFLHåndterer(beregningsgrunnlagRepository);
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_AT() {
        //Arrange
        buildOgLagreBeregningsgrunnlag(true, 1, 1);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(beregningsgrunnlag -> assertBeregningsgrunnlag(beregningsgrunnlag, 1, OVERSTYRT_PR_AR));
    }

    @Test
    public void skal_oppdatere_bruttoPrÅr_i_beregningsgrunnlagperiode_når_andel_overstyres_AT() {
        //Arrange
        int overstyrt1 = 1000;
        int overstyrt2 = 2000;
        int antallAndeler = 2;
        buildOgLagreBeregningsgrunnlag(true, 1, antallAndeler);

        List<InntektPrAndelDto> overstyrteVerdier = new ArrayList<>();
        overstyrteVerdier.add(new InntektPrAndelDto(overstyrt1, 1L));
        overstyrteVerdier.add(new InntektPrAndelDto(overstyrt2, 2L));

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", overstyrteVerdier, FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(beregningsgrunnlag -> {
            BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
            List<BeregningsgrunnlagPrStatusOgAndel> andeler = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
            assertThat(andeler).hasSize(antallAndeler);
            BigDecimal nyBruttoBG1 = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                .getBruttoPrÅr();
            assertThat(nyBruttoBG1.intValue()).as("nyBruttoBG").isEqualTo(overstyrt1);
            BigDecimal nyBruttoBG2 = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(1)
                .getBruttoPrÅr();
            assertThat(nyBruttoBG2.intValue()).as("nyBruttoBG").isEqualTo(overstyrt2);
            assertThat(beregningsgrunnlagPeriode.getBruttoPrÅr().intValue()).as("nyBruttoBGPeriode").isEqualTo(overstyrt1 + overstyrt2);
        });
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_for_fleire_perioder_med_andeler_med_ulike_inntektskategorier_AT() {
        //Arrange
        List<List<Boolean>> arbeidstakerPrPeriode = List.of(List.of(false, true), List.of(false, true, true, true));
        List<Inntektskategori> inntektskategoriPeriode1 = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.ARBEIDSTAKER);
        List<Inntektskategori> inntektskategoriPeriode2 = List.of(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.SJØMANN);
        List<List<Inntektskategori>> inntektskategoriPrPeriode = List.of(inntektskategoriPeriode1, inntektskategoriPeriode2);
        buildOgLagreBeregningsgrunnlag(arbeidstakerPrPeriode, inntektskategoriPrPeriode);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 2L)), FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        List<BeregningsgrunnlagPeriode> perioder = beregningsgrunnlagOpt.get().getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerPeriode1 = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerPeriode1).hasSize(2);
        assertThat(andelerPeriode1.get(1).getBruttoPrÅr().intValue()).isEqualByComparingTo(OVERSTYRT_PR_AR);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerPeriode2 = perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerPeriode2).hasSize(4);
        assertThat(andelerPeriode2.get(1).getBruttoPrÅr().intValue()).isEqualByComparingTo(OVERSTYRT_PR_AR);
        assertThat(andelerPeriode2.get(2).getBruttoPrÅr().intValue()).isEqualTo(BRUTTO_PR_AR);
        assertThat(andelerPeriode2.get(3).getBruttoPrÅr().intValue()).isEqualTo(BRUTTO_PR_AR);
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_for_fleire_perioder_AT() {
        //Arrange
        int antallPerioder = 3;
        buildOgLagreBeregningsgrunnlag(true, antallPerioder, 1);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(beregningsgrunnlag -> assertBeregningsgrunnlag(beregningsgrunnlag, antallPerioder, OVERSTYRT_PR_AR));
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_FL() {
        //Arrange
        buildOgLagreBeregningsgrunnlag(false, 1, 1);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(beregningsgrunnlag -> assertBeregningsgrunnlag(beregningsgrunnlag, 1, FRILANSER_INNTEKT));
    }

    @Test
    public void skal_oppdatere_beregningsgrunnlag_med_overstyrt_verdi_for_fleire_perioder_FL() {
        //Arrange
        int antallPerioder = 3;
        buildOgLagreBeregningsgrunnlag(false, antallPerioder, 1);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), FRILANSER_INNTEKT);

        // Act
        oppdaterer.håndter(behandlingReferanse.getId(), dto);

        //Assert
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagOpt).hasValueSatisfying(beregningsgrunnlag -> assertBeregningsgrunnlag(beregningsgrunnlag, antallPerioder, FRILANSER_INNTEKT));
    }

    private void assertBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag, int antallPerioder, int frilanserInntekt) {
        List<no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode> beregningsgrunnlagperioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(beregningsgrunnlagperioder).hasSize(antallPerioder);
        beregningsgrunnlagperioder.forEach(periode -> {
                BigDecimal nyBruttoBG = periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0)
                    .getBruttoPrÅr();
                assertThat(nyBruttoBG.intValue()).as("nyBruttoBG").isEqualTo(frilanserInntekt);
            }
        );
    }


    private void buildOgLagreBeregningsgrunnlag(List<List<Boolean>> erArbeidstakerPrPeriode, List<List<Inntektskategori>> inntektskategoriPrPeriode) {
        TestScenarioBuilder scenario = lagScenario();

        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = scenario.medBeregningsgrunnlag()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        assertThat(erArbeidstakerPrPeriode).hasSize(inntektskategoriPrPeriode.size());
        for (int i = 0; i < erArbeidstakerPrPeriode.size(); i++) {
            LocalDate fom = LocalDate.now().minusDays(20).plusDays(i * 5).plusDays(i == 0 ? 0 : 1);
            LocalDate tom = fom.plusDays(5);
            leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, fom, tom, erArbeidstakerPrPeriode.get(i), inntektskategoriPrPeriode.get(i));
        }
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private void buildOgLagreBeregningsgrunnlag(boolean erArbeidstaker, int antallPerioder, int antallAndeler) {
        TestScenarioBuilder scenario = lagScenario();
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = scenario.medBeregningsgrunnlag()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        for (int i = 0; i < antallPerioder; i++) {
            LocalDate fom = LocalDate.now().minusDays(20).plusDays(i * 5).plusDays(i == 0 ? 0 : 1);
            LocalDate tom = fom.plusDays(5);
            leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, fom, tom, erArbeidstaker, antallAndeler);
        }
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private TestScenarioBuilder lagScenario() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        return scenario;
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato, LocalDate tomDato, List<Boolean> erArbeidstakerList, List<Inntektskategori> inntektskategoriList) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fomDato, tomDato);
        assertThat(erArbeidstakerList).hasSize(inntektskategoriList.size());
        for (int i = 0; i < erArbeidstakerList.size(); i++) {
            if (erArbeidstakerList.get(i)) {
                leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, AktivitetStatus.ARBEIDSTAKER, virksomheter.get(0), ARBEIDSFORHOLD_ID, (long) (i+1), inntektskategoriList.get(i));
            } else {
                leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, AktivitetStatus.FRILANSER, null, null, ((long) (i+1)), inntektskategoriList.get(i));
            }
        }
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }


    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato, LocalDate tomDato, boolean erArbeidstaker, int antallAndeler) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fomDato, tomDato);
        for (int i = 0; i < antallAndeler; i++) {
            if (erArbeidstaker) {
                leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, AktivitetStatus.ARBEIDSTAKER, virksomheter.get(i), ARBEIDSFORHOLD_ID, (long) (i+1), Inntektskategori.ARBEIDSTAKER);
            } else {
                leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, AktivitetStatus.FRILANSER, null, null, ((long) (i+1)), Inntektskategori.FRILANSER);
            }
        }
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder, AktivitetStatus aktivitetStatus,
                                                          Virksomhet virksomheten, InternArbeidsforholdRef arbforholdId, Long andelsnr, Inntektskategori inntektskategori) {

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(andelsnr)
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(aktivitetStatus)
            .medBeregnetPrÅr(BigDecimal.valueOf(BRUTTO_PR_AR));
        if (virksomheten != null) {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsforholdRef(arbforholdId)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomheten.getOrgnr()))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            builder.medBGAndelArbeidsforhold(bga);
        }
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(
            builder);
    }
}
