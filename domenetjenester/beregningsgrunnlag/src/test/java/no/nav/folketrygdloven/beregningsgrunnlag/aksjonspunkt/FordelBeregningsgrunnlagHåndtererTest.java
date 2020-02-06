package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsatteVerdierDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.RedigerbarAndelDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FordelBeregningsgrunnlagHåndtererTest {
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);
    private static final String ORG_NUMMER = "915933149";


    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());


    @Inject
    private FordelRefusjonTjeneste fordelRefusjonTjeneste;
    private FordelBeregningsgrunnlagHåndterer fordelBeregningsgrunnlagHåndterer;

    public TestScenarioBuilder scenario;
    public BehandlingReferanse behandlingReferanse;

    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repositoryRule.getEntityManager());

    @Before
    public void setup() {
        this.scenario = TestScenarioBuilder.nyttScenario();
        this.behandlingReferanse = scenario.lagre(repositoryProvider);
        this.fordelBeregningsgrunnlagHåndterer = new FordelBeregningsgrunnlagHåndterer(beregningsgrunnlagRepository, fordelRefusjonTjeneste);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        return BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
    }

    private BeregningsgrunnlagPeriode lagPeriode(BeregningsgrunnlagEntitet forrigeBG, LocalDate fom, LocalDate tom) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .build(forrigeBG);
    }

    @Test
    public void skal_opprettholde_andelsnummer() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        Long andelsnr3 = 12L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        BeregningsgrunnlagPrStatusOgAndel andel2 = buildArbeidstakerAndel(arbId2, andelsnr2, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        BeregningsgrunnlagPrStatusOgAndel andel3 = buildArbeidstakerAndel(arbId2, andelsnr3, periode, null, true, Inntektskategori.FISKER, false, null);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);

        Integer fastsatt = 10_000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        Inntektskategori inntektskategori2 = Inntektskategori.DAGPENGER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, false, false, null, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(andel2, arbId2, andelsnr2, false, false, null, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = lagFordeltAndel(andel3, arbId2, andelsnr3, false, true, null, fastsatt, inntektskategori2);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel4 = lagFordeltAndel(null, arbId, andelsnr, true, true, null, fastsatt, inntektskategori2);

        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel4, fordeltAndel3, fordeltAndel, fordeltAndel2), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());

        BeregningsgrunnlagEntitet grunnlagEtterOppdatering = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        List<BeregningsgrunnlagPrStatusOgAndel> andeler = grunnlagEtterOppdatering.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList();

        BeregningsgrunnlagPrStatusOgAndel andelOppdatert1 = andeler.stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(inntektskategori)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andelOppdatert2 = andeler.stream().filter(a -> a.matchUtenInntektskategori(andel2) && a.getInntektskategori().equals(inntektskategori)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andelOppdatert3 = andeler.stream().filter(a -> a.matchUtenInntektskategori(andel3) && a.getInntektskategori().equals(inntektskategori2)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andelOppdatert4 = andeler.stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(inntektskategori2)).findFirst().get();

        // Assert
        assertThat(andelOppdatert1.getAndelsnr()).isEqualByComparingTo(andelsnr);
        assertThat(andelOppdatert2.getAndelsnr()).isEqualByComparingTo(andelsnr2);
        assertThat(andelOppdatert3.getAndelsnr()).isEqualByComparingTo(andelsnr3);
        assertThat(andelOppdatert4.getAndelsnr()).isEqualByComparingTo(andelsnr3 + 1L);

    }

    @Test
    public void skal_sette_verdier_på_DP_lagt_til_av_saksbehandler() {
        // Arrange
        Long andelsnr = 1L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildAPAndel(andelsnr, periode, true, true, BigDecimal.valueOf(100_000));

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);

        Integer fastsatt = 10_000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltDPAndel(andel, andelsnr, false, true, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(singletonList(fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());

        BeregningsgrunnlagEntitet grunnlagEtterOppdatering = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        BeregningsgrunnlagPrStatusOgAndel andelEtterOppdatering = grunnlagEtterOppdatering.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        // Assert
        assertThat(andelEtterOppdatering.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(andelEtterOppdatering.getInntektskategori()).isEqualTo(inntektskategori);

    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_med_1_periode_og_1_andel_refusjon_lik_null() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT_INN);

        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = null;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(singletonList(fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());

        BeregningsgrunnlagEntitet grunnlagEtterOppdatering = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        BeregningsgrunnlagPrStatusOgAndel andelEtterOppdatering = grunnlagEtterOppdatering.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        // Assert
        assertThat(andelEtterOppdatering.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(andelEtterOppdatering.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(andelEtterOppdatering.getInntektskategori()).isEqualTo(inntektskategori);

    }

    @Test
    public void skal_fordele_refusjon_etter_totalrefusjon_om_lik_null() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(120000);
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = buildArbeidstakerAndel(arbId, andelsnr, periode, refusjonskravPrÅr, false, Inntektskategori.ARBEIDSTAKER, false, null);
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel2 = buildArbeidstakerAndel(arbId2, andelsnr2, periode, refusjonskravPrÅr, false, Inntektskategori.ARBEIDSTAKER, false, null);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT_INN);

        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = null;
        Integer fastsatt = 30000;
        Integer fastsatt2 = 5000;
        Integer fastsatt3 = 5000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(eksisterendeAndel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(eksisterendeAndel2, arbId2, andelsnr2, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt2, inntektskategori);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = lagFordeltAndel(null, arbId2, andelsnr2, true, true, refusjon, fastsatt3, Inntektskategori.FRILANSER);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel, fordeltAndel2, fordeltAndel3), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());

        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBG.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andelsnr)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andel2 = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andelsnr2)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andel3 = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).findFirst().get();
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).get()).isEqualByComparingTo(refusjonskravPrÅr);
        assertThat(andel.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(andel.getInntektskategori()).isEqualTo(inntektskategori);
        BigDecimal halvRefusjon = refusjonskravPrÅr.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        assertThat(andel2.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).get()).isEqualByComparingTo(halvRefusjon);
        assertThat(andel2.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt2 * 12));
        assertThat(andel2.getInntektskategori()).isEqualTo(inntektskategori);
        assertThat(andel3.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).get()).isEqualByComparingTo(halvRefusjon);
        assertThat(andel3.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt3 * 12));
        assertThat(andel3.getInntektskategori()).isEqualTo(Inntektskategori.FRILANSER);
    }

    private FastsettBeregningsgrunnlagAndelDto lagFordeltAndel(BeregningsgrunnlagPrStatusOgAndel andel, InternArbeidsforholdRef arbId, Long andelsnr, boolean nyAndel, boolean lagtTilAvSaksbehandler, Integer refusjon, Integer fastsatt, Inntektskategori inntektskategori) {
        FastsatteVerdierDto fastsatteVerdier = new FastsatteVerdierDto(refusjon, fastsatt, inntektskategori, null);
        RedigerbarAndelDto andelDto = new RedigerbarAndelDto(nyAndel, ORG_NUMMER, arbId, andelsnr, lagtTilAvSaksbehandler, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
        return new FastsettBeregningsgrunnlagAndelDto(andelDto, fastsatteVerdier, Inntektskategori.ARBEIDSTAKER,
            andel != null ? andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO).intValue() : null,
            andel != null ? finnBrutto(andel) : null);
    }

    private FastsettBeregningsgrunnlagAndelDto lagFordeltDPAndel(BeregningsgrunnlagPrStatusOgAndel andel, Long andelsnr, boolean nyAndel, boolean lagtTilAvSaksbehandler,  Integer fastsatt, Inntektskategori inntektskategori) {
        FastsatteVerdierDto fastsatteVerdier = new FastsatteVerdierDto(null, fastsatt, inntektskategori, null);
        RedigerbarAndelDto andelDto = new RedigerbarAndelDto(nyAndel, andelsnr, lagtTilAvSaksbehandler, AktivitetStatus.DAGPENGER, OpptjeningAktivitetType.DAGPENGER);
        return new FastsettBeregningsgrunnlagAndelDto(andelDto, fastsatteVerdier, Inntektskategori.DAGPENGER, null,
            andel != null ? finnBrutto(andel) : null);
    }

    private Integer finnBrutto(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBruttoPrÅr() == null ? null : andel.getBruttoPrÅr().intValue();
    }

    private BeregningsgrunnlagPrStatusOgAndel buildArbeidstakerAndel(InternArbeidsforholdRef arbId2, Long andelsnr2, BeregningsgrunnlagPeriode periode,
                                                                     BigDecimal refusjonskravPrÅr, boolean lagtTilAvSaksbehandler,
                                                                     Inntektskategori inntektskategori, boolean fastsattAvSaksbehandler, BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORG_NUMMER))
                .medArbeidsforholdRef(arbId2).medRefusjonskravPrÅr(refusjonskravPrÅr))
            .medAndelsnr(andelsnr2)
            .medBeregningsperiode(LocalDate.of(2019,7,1), LocalDate.of(2019,10,1))
            .medBeregnetPrÅr(beregnetPrÅr)
            .medLagtTilAvSaksbehandler(lagtTilAvSaksbehandler)
            .medFastsattAvSaksbehandler(fastsattAvSaksbehandler)
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
    }

    private BeregningsgrunnlagPrStatusOgAndel buildAPAndel(Long andelsnr2, BeregningsgrunnlagPeriode periode,
                                                           boolean lagtTilAvSaksbehandler,
                                                           boolean fastsattAvSaksbehandler, BigDecimal beregnetPrÅr) {
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(andelsnr2)
            .medBeregningsperiode(LocalDate.of(2019,7,1), LocalDate.of(2019,10,1))
            .medBeregnetPrÅr(beregnetPrÅr)
            .medLagtTilAvSaksbehandler(lagtTilAvSaksbehandler)
            .medFastsattAvSaksbehandler(fastsattAvSaksbehandler)
            .medInntektskategori(Inntektskategori.ARBEIDSAVKLARINGSPENGER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .build(periode);
    }

    @Test
    public void skal_sette_verdier_på_andel_som_eksisterte_fra_før_i_grunnlag_med_1_periode_og_2_andeler() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel1 = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        buildArbeidstakerAndel(arbId2, andelsnr2, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel1, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(singletonList(fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        BeregningsgrunnlagPrStatusOgAndel andelEtterOppdatering = nyttBG.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        // Assert
        assertThat(andelEtterOppdatering.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null)).isEqualByComparingTo(BigDecimal.valueOf(refusjon * 12));
        assertThat(andelEtterOppdatering.getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(andelEtterOppdatering.getInntektskategori()).isEqualTo(inntektskategori);
    }


    @Test
    public void skal_sette_verdier_på_ny_andel_med_1_periode_og_1_andel() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);

        final boolean nyAndel2 = true;
        final boolean lagtTilAvSaksbehandler2 = true;
        Integer refusjon2 = 3000;
        Integer fastsatt2 = 20000;
        Inntektskategori inntektskategori2 = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(null, arbId, andelsnr, nyAndel2, lagtTilAvSaksbehandler2, refusjon2, fastsatt2, inntektskategori2);

        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel2, fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert
        double totalFastsatt = fastsatt + fastsatt2;
        double totalRefusjon = refusjon + refusjon2;

        assertThat(nyttBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(nyttBG.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(inntektskategori)).collect(Collectors.toList());

        assertThat(eksisterendeAndel).hasSize(1);
        assertThat(eksisterendeAndel.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf((fastsatt/totalFastsatt)*totalRefusjon * 12));
        assertThat(eksisterendeAndel.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel.get(0).getInntektskategori()).isEqualTo(inntektskategori);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());

        assertThat(andelLagtTil).hasSize(1);
        assertThat(andelLagtTil.get(0).getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(andelLagtTil.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf((fastsatt2/totalFastsatt)*totalRefusjon * 12));
        assertThat(andelLagtTil.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt2 * 12));
        assertThat(andelLagtTil.get(0).getInntektskategori()).isEqualTo(inntektskategori2);
        assertThat(andelLagtTil.get(0).getBeregningsperiodeFom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeFom());
        assertThat(andelLagtTil.get(0).getBeregningsperiodeTom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeTom());
    }


    @Test
    public void skal_sette_verdier_på_andeler_for_tilbakehopp_til_KOFAKBER_med_1_periode_og_1_andel() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnrForAndelLagtTilAvSaksbehandler = 2L;

        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER,
            false, null);
        BeregningsgrunnlagEntitet forrigeGrunnlag = beregningsgrunnlag.dypKopi();
        forrigeGrunnlag = buildArbeidstakerAndel(arbId, andelsnrForAndelLagtTilAvSaksbehandler, forrigeGrunnlag.getBeregningsgrunnlagPerioder().get(0),
            null, false, Inntektskategori.SJØMANN, false, null)
            .getBeregningsgrunnlagPeriode().getBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeGrunnlag, BeregningsgrunnlagTilstand.FASTSATT_INN);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler,
            refusjon, fastsatt, inntektskategori);

        final boolean nyAndel2 = false;
        final boolean lagtTilAvSaksbehandler2 = true;
        Integer refusjon2 = 3000;
        Integer fastsatt2 = 20000;
        Inntektskategori inntektskategori2 = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(null, arbId, andelsnrForAndelLagtTilAvSaksbehandler, nyAndel2, lagtTilAvSaksbehandler2,
            refusjon2, fastsatt2, inntektskategori2);


        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel2, fordeltAndel),
            SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        // Assert
        double totalRefusjon = refusjon + refusjon2;
        double totalFastsatt = fastsatt + fastsatt2;
        assertThat(nyttBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(nyttBG.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(inntektskategori)).collect(Collectors.toList());

        assertThat(eksisterendeAndel).hasSize(1);
        assertThat(eksisterendeAndel.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt/totalFastsatt*totalRefusjon * 12));
        assertThat(eksisterendeAndel.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel.get(0).getInntektskategori()).isEqualTo(inntektskategori);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());

        assertThat(andelLagtTil).hasSize(1);
        assertThat(andelLagtTil.get(0).getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(andelLagtTil.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt2/totalFastsatt*totalRefusjon * 12));
        assertThat(andelLagtTil.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt2 * 12));
        assertThat(andelLagtTil.get(0).getInntektskategori()).isEqualTo(inntektskategori2);
        assertThat(andelLagtTil.get(0).getBeregningsperiodeFom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeFom());
        assertThat(andelLagtTil.get(0).getBeregningsperiodeTom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeTom());

    }


    @Test
    public void skal_sette_verdier_på_andeler_for_tilbakehopp_til_steg_før_KOFAKBER() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        BigDecimal forrigeFastsatt = BigDecimal.valueOf(200000);
        Inntektskategori forrigeInntektskategori = Inntektskategori.SJØMANN;
        BeregningsgrunnlagEntitet forrigeBG = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periodeForrigeBG = lagPeriode(forrigeBG, SKJÆRINGSTIDSPUNKT, null);
        buildArbeidstakerAndel(arbId, andelsnr, periodeForrigeBG, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        buildArbeidstakerAndel(arbId, andelsnr2, periodeForrigeBG, null, true, forrigeInntektskategori,
            true, forrigeFastsatt);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBG, BeregningsgrunnlagTilstand.FASTSATT_INN);

        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER,
            false, null);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);

        final boolean nyAndel2 = false;
        final boolean lagtTilAvSaksbehandler2 = true;
        Integer refusjon2 = 3000;
        Integer fastsatt2 = 20000;
        Inntektskategori inntektskategori2 = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(null, arbId, andelsnr2, nyAndel2, lagtTilAvSaksbehandler2, refusjon2, fastsatt2, inntektskategori2);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel, fordeltAndel2), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert
        double totalRefusjon = refusjon + refusjon2;
        double totalFastsatt = fastsatt + fastsatt2;

        assertThat(nyttBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(nyttBG.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andelsnr)).collect(Collectors.toList());

        assertThat(eksisterendeAndel).hasSize(1);
        assertThat(eksisterendeAndel.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt/totalFastsatt*totalRefusjon * 12));
        assertThat(eksisterendeAndel.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel.get(0).getInntektskategori()).isEqualTo(inntektskategori);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());

        assertThat(andelLagtTil).hasSize(1);
        assertThat(andelLagtTil.get(0).getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(andelLagtTil.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt2/totalFastsatt*totalRefusjon  * 12));
        assertThat(andelLagtTil.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt2 * 12));
        assertThat(andelLagtTil.get(0).getInntektskategori()).isEqualTo(inntektskategori2);
        assertThat(andelLagtTil.get(0).getBeregningsperiodeFom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeFom());
        assertThat(andelLagtTil.get(0).getBeregningsperiodeTom()).isEqualTo(eksisterendeAndel.get(0).getBeregningsperiodeTom());
    }


    @Test
    public void skal_ikkje_legge_til_slettet_andel_ved_tilbakehopp_til_steg_før_KOFAKBER() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        BigDecimal forrigeFastsatt = BigDecimal.valueOf(200000);
        Inntektskategori forrigeInntektskategori = Inntektskategori.SJØMANN;
        BeregningsgrunnlagEntitet forrigeBG = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periodeForrigeBG = lagPeriode(forrigeBG, SKJÆRINGSTIDSPUNKT, null);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periodeForrigeBG, null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        buildArbeidstakerAndel(arbId, andelsnr2, periodeForrigeBG, null, true, forrigeInntektskategori,
            true, forrigeFastsatt);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBG, BeregningsgrunnlagTilstand.FASTSATT_INN);

        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode = lagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT, null);
        buildArbeidstakerAndel(arbId, andelsnr, periode, null, false, Inntektskategori.ARBEIDSTAKER, false,null);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);

        FastsettBeregningsgrunnlagPeriodeDto endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel), SKJÆRINGSTIDSPUNKT, null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(singletonList(endretPeriode), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        // Assert
        assertThat(nyttBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(nyttBG.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andelsnr)).collect(Collectors.toList());

        assertThat(eksisterendeAndel).hasSize(1);
        assertThat(eksisterendeAndel.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(refusjon * 12));
        assertThat(eksisterendeAndel.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel.get(0).getInntektskategori()).isEqualTo(inntektskategori);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil = nyttBG.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler)
            .collect(Collectors.toList());

        assertThat(andelLagtTil).isEmpty();
    }


    @Test
    public void skal_sette_verdier_på_andeler_for_tilbakehopp_til_steg_før_KOFAKBER_med_nye_andeler_og_eksisterende_andeler_i_ulike_arbeidsforhold() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        Long andelsnr = 1L;
        Long andelsnr2 = 2L;
        Long andelsnr3 = 3L;

        BigDecimal forrigeFastsatt = BigDecimal.valueOf(200000);
        Inntektskategori forrigeInntektskategori = Inntektskategori.SJØMANN;
        BeregningsgrunnlagEntitet forrigeBG = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periodeForrigeBG = lagPeriode(forrigeBG, SKJÆRINGSTIDSPUNKT, null);
        buildArbeidstakerAndel(arbId, andelsnr, periodeForrigeBG,
            null, false, Inntektskategori.ARBEIDSTAKER, false,null);
        buildArbeidstakerAndel(arbId, andelsnr2, periodeForrigeBG,
            null, true, forrigeInntektskategori, true, forrigeFastsatt);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBG, BeregningsgrunnlagTilstand.FASTSATT_INN);

        BeregningsgrunnlagEntitet beregningsgrunnlag = lagBeregningsgrunnlag();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        buildArbeidstakerAndel(arbId, andelsnr, periode1,
            null, false, Inntektskategori.ARBEIDSTAKER, false,null);
        BeregningsgrunnlagPeriode periode2 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(2), null)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = buildArbeidstakerAndel(arbId, andelsnr, periode2,
            null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        BeregningsgrunnlagPrStatusOgAndel andel3 = buildArbeidstakerAndel(arbId2, andelsnr3, periode2,
            null, false, Inntektskategori.ARBEIDSTAKER, false, null);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        final boolean nyAndel = false;
        final boolean lagtTilAvSaksbehandler = false;
        Integer refusjon = 5000;
        Integer fastsatt = 10000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = lagFordeltAndel(andel, arbId, andelsnr, nyAndel, lagtTilAvSaksbehandler, refusjon, fastsatt, inntektskategori);

        final boolean nyAndel2 = false;
        final boolean lagtTilAvSaksbehandler2 = true;
        Integer refusjon2 = 3000;
        Integer fastsatt2 = 20000;
        Inntektskategori inntektskategori2 = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = lagFordeltAndel(null, arbId, andelsnr2, nyAndel2, lagtTilAvSaksbehandler2, refusjon2, fastsatt2, inntektskategori2);

        final boolean nyAndel3 = true;
        final boolean lagtTilAvSaksbehandler3 = true;
        Integer refusjon3 = 2000;
        Integer fastsatt3 = 30000;
        Inntektskategori inntektskategori3 = Inntektskategori.JORDBRUKER;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = lagFordeltAndel(null, arbId, andelsnr, nyAndel3, lagtTilAvSaksbehandler3, refusjon3, fastsatt3, inntektskategori3);

        FastsettBeregningsgrunnlagPeriodeDto endretPeriode1 = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel3, fordeltAndel, fordeltAndel2), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1));

        final boolean nyAndel4 = false;
        final boolean lagtTilAvSaksbehandler4 = false;
        Integer refusjon4 = 10000;
        Integer fastsatt4 = 40000;
        Inntektskategori inntektskategori4 = Inntektskategori.SJØMANN;
        FastsettBeregningsgrunnlagAndelDto fordeltAndel4 = lagFordeltAndel(andel3, arbId2, andelsnr3, nyAndel4, lagtTilAvSaksbehandler4, refusjon4, fastsatt4, inntektskategori4);
        FastsettBeregningsgrunnlagPeriodeDto endretPeriode2 = new FastsettBeregningsgrunnlagPeriodeDto(List.of(fordeltAndel3, fordeltAndel, fordeltAndel4), SKJÆRINGSTIDSPUNKT.plusMonths(2), null);
        FordelBeregningsgrunnlagDto dto = new FordelBeregningsgrunnlagDto(List.of(endretPeriode2, endretPeriode1), "Begrunnelse");

        // Act
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBG = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert
        double totalRefusjon1 = refusjon + refusjon2 + refusjon3;
        double totalFastsatt1 = fastsatt + fastsatt2 + fastsatt3;
        double totalRefusjon2 = refusjon  + refusjon3;
        double totalFastsatt2 = fastsatt  + fastsatt3;

        assertThat(nyttBG.getBeregningsgrunnlagPerioder()).hasSize(2);

        BeregningsgrunnlagPeriode periode1Oppdatert = nyttBG.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPeriode periode2Oppdatert = nyttBG.getBeregningsgrunnlagPerioder().get(1);

        assertThat(periode1Oppdatert.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel = periode1Oppdatert
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(fordeltAndel.getFastsatteVerdier().getInntektskategori())).collect(Collectors.toList());

        assertThat(eksisterendeAndel).hasSize(1);
        assertThat(eksisterendeAndel.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt/totalFastsatt1*totalRefusjon1 * 12));
        assertThat(eksisterendeAndel.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel.get(0).getInntektskategori()).isEqualTo(inntektskategori);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil = periode1Oppdatert
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());

        assertThat(andelLagtTil).hasSize(2);
        Optional<BeregningsgrunnlagPrStatusOgAndel> fraForrige = andelLagtTil.stream().filter(lagtTil -> lagtTil.getInntektskategori().equals(inntektskategori2)).findFirst();
        assertThat(fraForrige.isPresent()).isTrue();
        assertThat(fraForrige.get().getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(fraForrige.get().getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt2/totalFastsatt1*totalRefusjon1 * 12));
        assertThat(fraForrige.get().getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt2 * 12));
        assertThat(fraForrige.get().getInntektskategori()).isEqualTo(inntektskategori2);

        Optional<BeregningsgrunnlagPrStatusOgAndel> ny = andelLagtTil.stream().filter(lagtTil -> lagtTil.getInntektskategori().equals(inntektskategori3)).findFirst();
        assertThat(ny.isPresent()).isTrue();
        assertThat(ny.get().getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(ny.get().getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt3/totalFastsatt1*totalRefusjon1 * 12));
        assertThat(ny.get().getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt3 * 12));
        assertThat(ny.get().getInntektskategori()).isEqualTo(inntektskategori3);

        assertThat(periode2Oppdatert.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel2 = periode2Oppdatert
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.matchUtenInntektskategori(andel) && a.getInntektskategori().equals(fordeltAndel.getFastsatteVerdier().getInntektskategori())).collect(Collectors.toList());
        assertThat(eksisterendeAndel2).hasSize(1);
        assertThat(eksisterendeAndel2.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt/totalFastsatt2*totalRefusjon2 * 12));
        assertThat(eksisterendeAndel2.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt * 12));
        assertThat(eksisterendeAndel2.get(0).getInntektskategori()).isEqualTo(inntektskategori);

        List<BeregningsgrunnlagPrStatusOgAndel> eksisterendeAndel3 = periode2Oppdatert
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.matchUtenInntektskategori(andel3) && a.getInntektskategori().equals(fordeltAndel4.getFastsatteVerdier().getInntektskategori())).collect(Collectors.toList());
        assertThat(eksisterendeAndel3).hasSize(1);
        assertThat(eksisterendeAndel3.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(refusjon4 * 12));
        assertThat(eksisterendeAndel3.get(0).getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt4 * 12));
        assertThat(eksisterendeAndel3.get(0).getInntektskategori()).isEqualTo(inntektskategori4);


        List<BeregningsgrunnlagPrStatusOgAndel> andelLagtTil2 = periode2Oppdatert
            .getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler).collect(Collectors.toList());

        assertThat(andelLagtTil2).hasSize(1);
        Optional<BeregningsgrunnlagPrStatusOgAndel> fraForrige2 = andelLagtTil2.stream()
            .filter(lagtTil -> lagtTil.getInntektskategori().equals(inntektskategori2)).findFirst();
        assertThat(fraForrige2.isPresent()).isFalse();

        Optional<BeregningsgrunnlagPrStatusOgAndel> ny2 = andelLagtTil2.stream().filter(lagtTil -> lagtTil.getInntektskategori().equals(inntektskategori3)).findFirst();
        assertThat(ny2.isPresent()).isTrue();
        assertThat(ny2.get().getAndelsnr()).isNotEqualTo(eksisterendeAndel.get(0).getAndelsnr());
        assertThat(ny2.get().getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.valueOf(fastsatt3/totalFastsatt2*totalRefusjon2 * 12));
        assertThat(ny2.get().getFordeltPrÅr()).isEqualByComparingTo(BigDecimal.valueOf(fastsatt3 * 12));
        assertThat(ny2.get().getInntektskategori()).isEqualTo(inntektskategori3);
    }

}

