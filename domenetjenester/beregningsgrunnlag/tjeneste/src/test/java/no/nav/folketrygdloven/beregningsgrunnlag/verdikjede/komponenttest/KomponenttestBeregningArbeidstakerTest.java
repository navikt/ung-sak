package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest;

import static java.math.BigDecimal.ZERO;
import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest.KomponenttestBeregningAssertUtil.assertBeregningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest.KomponenttestBeregningAssertUtil.assertBeregningsgrunnlagAndel;
import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest.KomponenttestBeregningAssertUtil.assertBeregningsgrunnlagPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest.KomponenttestBeregningAssertUtil.assertSammenligningsgrunnlag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.InntektPrAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningArbeidsgiverTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.konfig.Tid;

@RunWith(CdiRunner.class)
public class KomponenttestBeregningArbeidstakerTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.DECEMBER, 1);
    private static final LocalDateTime INNSENDINGSTIDSPUNKT = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(1).atStartOfDay();
    private static final String ORGNR = "974761076";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repoRule.getEntityManager());

    @Inject
    private BeregningInntektsmeldingTestUtil inntektsmeldingUtil;
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningArbeidsgiverTestUtil arbeidsgiverTestUtil;
    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    private FastsettBeregningsgrunnlagATFLHåndterer fastsettBeregningsgrunnlagATFLHåndterer;

    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider).medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .build());
        fastsettBeregningsgrunnlagATFLHåndterer = new FastsettBeregningsgrunnlagATFLHåndterer(beregningsgrunnlagRepository);

    }

    // Arbeidsgivere: 1 (virksomhet)
    // Arbeidsforhold: 1
    // Inntekt: < 6G
    // Refusjon: Full
    // Aksjonspunkt: Nei
    @Test
    public void skal_utføre_beregning_for_arbeidstaker_uten_aksjonspunkter() {
        // Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        BigDecimal inntektPrMnd = BigDecimal.valueOf(35000L);
        BigDecimal inntektPrÅr = inntektPrMnd.multiply(BigDecimal.valueOf(12L));
        BigDecimal refusjonPrMnd = BigDecimal.valueOf(35000L);
        BigDecimal refusjonskravPrÅr = refusjonPrMnd.multiply(BigDecimal.valueOf(12L));

        Periode periode = Periode.of(Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 7).getFom(), null);

        iayTestUtil.byggArbeidForBehandlingMedVirksomhetPåInntekt(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(7),
            Tid.TIDENES_ENDE, arbeidsforholdId, arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(ORGNR), inntektPrMnd);
        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT_OPPTJENING, refusjonPrMnd, inntektPrMnd, INNSENDINGSTIDSPUNKT);
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, periode, ORGNR);
        List<Inntektsmelding> inntektsmeldinger = List.of(im1);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act steg FastsettBeregningAktiviteter
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = doStegFastsettSkjæringstidspunkt(input);

        // Assert
        assertFastsettSkjæringstidspunktSteg(aksjonspunktResultater, true, ORGNR);

        // Act steg KontrollerFaktaBeregning
        aksjonspunktResultater = doStegKontrollerFaktaBeregning(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmBeregningSteg(aksjonspunktResultater);

        // Act steg ForeslåBeregningsgrunnlag
        aksjonspunktResultater = doStegForeslåBeregningsgrunnlag(input);

        // Assert steg ForeslåBeregningsgrunnlag
        assertForeslåBeregningsgrunnlagSteg(aksjonspunktResultater,
            inntektPrÅr,
            inntektPrÅr,
            0L,
            false);

        // Act steg fordel
        aksjonspunktResultater = doStegFordelBeregningsgrunnlag(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmFordelingSteg(aksjonspunktResultater, refusjonskravPrÅr, inntektPrÅr);

        // Act steg FastsettBeregningsgrunnlag
        doStegFastsettBeregningsgrunnlag(input);

        // Assert steg FastsettBeregningsgrunnlag
        // Assert steg FastsettBeregningsgrunnlag og oppdaterer
        assertFastsettBeregningsgrunnlag(refusjonskravPrÅr,
            inntektPrÅr,
            null);
    }

    // Arbeidsgivere: 1 (virksomhet)
    // Arbeidsforhold: 1
    // Inntekt: < 6G
    // Refusjon: Ingen
    // Aksjonspunkt: 5038 - FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS
    @Test
    public void skal_utføre_beregning_for_arbeidstaker_med_aksjonspunkter() {
        // Arrange
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        BigDecimal inntektIRegister = BigDecimal.valueOf(70000L);
        BigDecimal inntektFraIM = BigDecimal.valueOf(35000L);
        BigDecimal inntektPrÅrRegister = inntektIRegister.multiply(BigDecimal.valueOf(12L));
        BigDecimal inntektPrÅrIM = inntektFraIM.multiply(BigDecimal.valueOf(12L));
        Integer overstyrtPrÅr = 500000;

        Periode periode = Periode.of(Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 7).getFom(), null);

        iayTestUtil.byggArbeidForBehandlingMedVirksomhetPåInntekt(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(7),
            Tid.TIDENES_ENDE, arbeidsforholdId, arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(ORGNR), inntektIRegister);

        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT_OPPTJENING, ZERO, inntektFraIM, INNSENDINGSTIDSPUNKT);
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, periode, ORGNR);

        List<Inntektsmelding> inntektsmeldinger = List.of(im1);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act steg FastsettBeregningAktiviteter
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = doStegFastsettSkjæringstidspunkt(input);

        // Assert
        assertFastsettSkjæringstidspunktSteg(aksjonspunktResultater, true, ORGNR);

        // Act steg KontrollerFaktaBeregning
        aksjonspunktResultater = doStegKontrollerFaktaBeregning(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmBeregningSteg(aksjonspunktResultater);

        // Act steg ForeslåBeregningsgrunnlag
        aksjonspunktResultater = doStegForeslåBeregningsgrunnlag(input);

        // Assert steg ForeslåBeregningsgrunnlag
        assertForeslåBeregningsgrunnlagSteg(aksjonspunktResultater,
            inntektPrÅrRegister,
            inntektPrÅrIM,
            500L,
            true);

        // Act steg fordel
        aksjonspunktResultater = doStegFordelBeregningsgrunnlag(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmFordelingSteg(aksjonspunktResultater, ZERO, inntektPrÅrIM);

        // Act oppdaterer
        FastsettBeregningsgrunnlagATFLDto dto = lagATFLOppdatererDto(overstyrtPrÅr);
        fastsettBeregningsgrunnlagATFLHåndterer.håndter(behandlingReferanse.getId(), dto);

        // Act steg FastsettBeregningsgrunnlag
        doStegFastsettBeregningsgrunnlag(input);

        // Assert steg FastsettBeregningsgrunnlag og oppdaterer
        assertFastsettBeregningsgrunnlag(ZERO,
            inntektPrÅrIM,
            overstyrtPrÅr);
    }

    // Arbeidsgivere: 1 (privatperson)
    // Arbeidsforhold: 1
    // Inntekt: < 6G
    // Refusjon: Ingen
    // Aksjonspunkt: 5038 - FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS
    @Test
    public void skal_utføre_beregning_for_arbeidstaker_med_aksjonspunkter_arbeidsgiver_er_privatperson() {
        // Arrange
        BigDecimal inntektIRegister = BigDecimal.valueOf(70000L);
        BigDecimal inntektPrÅrRegister = inntektIRegister.multiply(BigDecimal.valueOf(12L));
        String arbeidsgiverAktørId = behandlingReferanse.getAktørId().getId();

        iayTestUtil.byggArbeidForBehandlingMedVirksomhetPåInntekt(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(7),
            Tid.TIDENES_ENDE, null, arbeidsgiverTestUtil.forArbeidsgiverpPrivatperson(behandlingReferanse.getAktørId()), inntektIRegister);

        Periode periode = Periode.of(Periode.månederFør(SKJÆRINGSTIDSPUNKT_OPPTJENING, 7).getFom(), null);

        var opptjeningAktiviteteter = OpptjeningAktiviteter.fraAktørId(OpptjeningAktivitetType.ARBEID, periode, arbeidsgiverAktørId);

        var iayGrunnlag = iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getId());
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteteter, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);

        // Act steg FastsettBeregningAktiviteter
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = doStegFastsettSkjæringstidspunkt(input);

        // Assert
        assertFastsettSkjæringstidspunktSteg(aksjonspunktResultater, false, arbeidsgiverAktørId);

        // Act steg KontrollerFaktaBeregning
        aksjonspunktResultater = doStegKontrollerFaktaBeregning(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmBeregningSteg(aksjonspunktResultater);

        // Act steg ForeslåBeregningsgrunnlag
        aksjonspunktResultater = doStegForeslåBeregningsgrunnlag(input);

        // Assert steg ForeslåBeregningsgrunnlag
        assertForeslåBeregningsgrunnlagSteg(aksjonspunktResultater,
            inntektPrÅrRegister,
            null,
            0L,
            false);

        // Act steg fordel
        aksjonspunktResultater = doStegFordelBeregningsgrunnlag(input);

        // Assert steg KontrollerFaktaBeregning
        assertKontrollerFaktaOmFordelingSteg(aksjonspunktResultater, ZERO, inntektPrÅrRegister);

        // Act steg FastsettBeregningsgrunnlag
        doStegFastsettBeregningsgrunnlag(input);

        // Assert steg FastsettBeregningsgrunnlag og oppdaterer
        assertFastsettBeregningsgrunnlag(BigDecimal.ZERO,
            inntektPrÅrRegister,
            null);
    }

    private void doStegFastsettBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        beregningsgrunnlagTjeneste.fastsettBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> doStegFordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(input);
        return utledAksjonspunkterForFordelBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> doStegForeslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(input).getAksjonspunkter();
    }

    private List<BeregningAksjonspunktResultat> utledAksjonspunkterForFordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.utledAksjonspunkterForFordelBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> doStegKontrollerFaktaBeregning(BeregningsgrunnlagInput input) {
        return kontrollerFaktaBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> doStegFastsettSkjæringstidspunkt(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(input);
    }

    private FastsettBeregningsgrunnlagATFLDto lagATFLOppdatererDto(Integer overstyrtPrÅr) {
        return new FastsettBeregningsgrunnlagATFLDto("Begrunnelse", lagInntektPrAndelDto(overstyrtPrÅr), null);
    }

    private List<InntektPrAndelDto> lagInntektPrAndelDto(Integer overstyrtPrÅr) {
        return Collections.singletonList(new InntektPrAndelDto(overstyrtPrÅr, 1L));
    }

    private void assertFastsettSkjæringstidspunktSteg(List<BeregningAksjonspunktResultat> aksjonspunktResultater, boolean erVirksomhet, String identifikator) {
        assertThat(aksjonspunktResultater).isEmpty();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId());
        assertThat(grunnlagOpt).hasValueSatisfying(grunnlag -> {
            List<BeregningAktivitetEntitet> beregningAktiviteter = grunnlag.getRegisterAktiviteter().getBeregningAktiviteter();
            assertThat(beregningAktiviteter).hasSize(1);
            assertThat(beregningAktiviteter.get(0)).satisfies(aktivitet -> {
                assertThat(aktivitet.getPeriode().getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(7));
                assertThat(aktivitet.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(aktivitet.getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
                assertThat(aktivitet.getArbeidsgiver().getErVirksomhet()).isEqualTo(erVirksomhet);
                assertThat(aktivitet.getArbeidsgiver().getIdentifikator()).isEqualTo(identifikator);
                assertThat(aktivitet.getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
            });
        });
    }

    private void assertKontrollerFaktaOmBeregningSteg(List<BeregningAksjonspunktResultat> aksjonspunktResultater) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());

        // Assert steg KontrollerFaktaBeregning
        assertThat(aksjonspunktResultater).isEmpty();
        assertThat(beregningsgrunnlagOpt).isPresent();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagOpt.get();
        assertBeregningsgrunnlag(beregningsgrunnlag,
            SKJÆRINGSTIDSPUNKT_OPPTJENING, Collections.singletonList(AktivitetStatus.ARBEIDSTAKER));

        // Beregningsgrunnlagperiode
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode førstePeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        assertBeregningsgrunnlagPeriode(førstePeriode,
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, null),
            ZERO,
            null,
            null,
            null);

        // BeregningsgrunnlagPrStatusOgAndel
        assertThat(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertBeregningsgrunnlagAndel(
            førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0),
            null,
            AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER,
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1),
            null, null);
    }

    private void assertKontrollerFaktaOmFordelingSteg(List<BeregningAksjonspunktResultat> aksjonspunktResultater, BigDecimal refusjonskravPrÅr,
                                                      BigDecimal beregnetPrÅr) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());

        // Assert steg KontrollerFaktaBeregning
        assertThat(aksjonspunktResultater).isEmpty();
        assertThat(beregningsgrunnlagOpt).isPresent();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagOpt.get();
        assertBeregningsgrunnlag(beregningsgrunnlag,
            SKJÆRINGSTIDSPUNKT_OPPTJENING, Collections.singletonList(AktivitetStatus.ARBEIDSTAKER));

        // Beregningsgrunnlagperiode
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode førstePeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        assertBeregningsgrunnlagPeriode(førstePeriode,
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, null),
            beregnetPrÅr,
            null,
            null,
            refusjonskravPrÅr);

        // BeregningsgrunnlagPrStatusOgAndel
        assertThat(førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertBeregningsgrunnlagAndel(
            førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0),
            beregnetPrÅr,
            AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER,
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1),
            refusjonskravPrÅr, null);
    }

    private void assertForeslåBeregningsgrunnlagSteg(List<BeregningAksjonspunktResultat> aksjonspunktResultater,
                                                     BigDecimal inntektPrÅrRegister,
                                                     BigDecimal inntektPrÅrIM,
                                                     Long avvik,
                                                     boolean medAksjonspunkt) {
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagEtterAndreSteg = beregningsgrunnlagRepository
            .hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());

        // Aksjonspunkter
        if (medAksjonspunkt) {
            assertThat(aksjonspunktResultater).isNotEmpty().hasSize(1);
            assertThat(aksjonspunktResultater.get(0).getBeregningAksjonspunktDefinisjon())
                .isEqualTo(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        } else {
            assertThat(aksjonspunktResultater).isEmpty();
        }
        assertThat(beregningsgrunnlagEtterAndreSteg).isPresent();

        // Sammenligningsgrunnlag
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagEtterAndreSteg.get();
        assertSammenligningsgrunnlag(beregningsgrunnlag.getSammenligningsgrunnlag(), inntektPrÅrRegister, avvik);

        BigDecimal gjeldendeInntekt = inntektPrÅrIM == null ? inntektPrÅrRegister : inntektPrÅrIM;
        // Periodenivå
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode førstePeriodeStegTo = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        assertBeregningsgrunnlagPeriode(førstePeriodeStegTo,
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, null), gjeldendeInntekt, null, null, BigDecimal.ZERO);

        // Andelsnivå
        for (BeregningsgrunnlagPeriode bgp : beregningsgrunnlag.getBeregningsgrunnlagPerioder()) {
            for (BeregningsgrunnlagPrStatusOgAndel andel : bgp.getBeregningsgrunnlagPrStatusOgAndelList()) {
                assertBeregningsgrunnlagAndel(andel,
                    gjeldendeInntekt,
                    AktivitetStatus.ARBEIDSTAKER,
                    Inntektskategori.ARBEIDSTAKER,
                    SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3),
                    SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1), null, null);
            }
        }
    }

    private void assertFastsettBeregningsgrunnlag(BigDecimal refusjonskravPrÅr,
                                                  BigDecimal beregnetPrÅr,
                                                  Integer overstyrtPrÅr) {
        // Beregningsgrunnlag
        Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagEtterTredjeSteg = beregningsgrunnlagRepository
            .hentBeregningsgrunnlagForBehandling(behandlingReferanse.getId());
        assertThat(beregningsgrunnlagEtterTredjeSteg).isPresent();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagEtterTredjeSteg.get();
        BigDecimal overstyrtViØnskerAssertPå = null;
        BigDecimal inntektDagsatsBeregnesFra = beregnetPrÅr;
        if (overstyrtPrÅr != null) {
            inntektDagsatsBeregnesFra = BigDecimal.valueOf(overstyrtPrÅr);
            overstyrtViØnskerAssertPå = BigDecimal.valueOf(overstyrtPrÅr);
        }
        BigDecimal forventetDagsats = inntektDagsatsBeregnesFra.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(2236));
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);

        // Periodenivå
        BeregningsgrunnlagPeriode førstePeriodeTredjeSteg = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        assertThat(førstePeriodeTredjeSteg.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertBeregningsgrunnlagPeriode(førstePeriodeTredjeSteg,
            ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, null),
            beregnetPrÅr,
            forventetDagsats.longValue(),
            overstyrtViØnskerAssertPå, refusjonskravPrÅr);

        // Andelsnivå
        BeregningsgrunnlagPrStatusOgAndel andel = førstePeriodeTredjeSteg.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertBeregningsgrunnlagAndel(andel,
            beregnetPrÅr,
            AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER,
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1),
            refusjonskravPrÅr,
            overstyrtViØnskerAssertPå);
    }
}
