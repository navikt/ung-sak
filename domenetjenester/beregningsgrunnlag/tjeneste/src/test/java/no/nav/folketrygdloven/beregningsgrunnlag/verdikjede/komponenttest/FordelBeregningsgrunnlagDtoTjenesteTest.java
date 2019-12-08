package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest;

import static java.util.stream.Collectors.toList;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FordelRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.FaktaOmFordelingDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.FordelBeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningArbeidsgiverTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FordelBeregningsgrunnlagDtoTjenesteTest {

    private static final String ARBEIDSGIVER_ORGNR = "991825827";
    private static final LocalDate STP_OPPTJENING = LocalDate.of(2019, Month.FEBRUARY, 10);
    private static final LocalDate FØRSTE_UTTAKSDATO = STP_OPPTJENING.plusDays(1);

    private static final Supplier<LocalDateTime> INNSENDINGSTIDSPUNKT = new Supplier<>() {
        // inkrementerer tidspunkt for hvert kall slik at det aldri blir samme
        LocalDateTime start = FØRSTE_UTTAKSDATO.atStartOfDay();
        int counter = 0;

        @Override
        public LocalDateTime get() {
            return start.plusSeconds(counter++);
        }
    };

    private static final BigDecimal INNTEKT_PR_MND = BigDecimal.valueOf(15000L);
    private static final int FASTSATT_REFUSJON = 20000;
    private static final int FASTSATT_BELØP = 12478;
    private static final Inntektskategori FASTSATT_INNTEKTSKATEGORI = Inntektskategori.JORDBRUKER;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    // Testdata tjenester
    @Inject
    private BeregningInntektsmeldingTestUtil inntektsmeldingUtil;
    @Inject
    private BeregningIAYTestUtil iayTestUtil;
    @Inject
    private BeregningArbeidsgiverTestUtil arbeidsgiverTestUtil;
    @Inject
    private BeregningsgrunnlagTestUtil testUtil;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Inject
    private FaktaOmFordelingDtoTjeneste faktaOmFordelingDtoTjeneste;
    @Inject
    private FordelBeregningsgrunnlagDtoTjeneste fordelBeregningsgrunnlagDtoTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private FordelBeregningsgrunnlagHåndterer fordelBeregningsgrunnlagHåndterer;

    private BeregningAktivitetAggregatEntitet.Builder beregningAktivitetBuilder = BeregningAktivitetAggregatEntitet.builder()
        .medSkjæringstidspunktOpptjening(STP_OPPTJENING);

    // Systems under test inkludert repositories
    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repoRule.getEntityManager());

    // Global state
    private int refusjonOver6GPrMnd;
    private BehandlingReferanse behandlingReferanse;

    @Before
    public void setup() {
        fordelBeregningsgrunnlagHåndterer = new FordelBeregningsgrunnlagHåndterer(beregningsgrunnlagRepository, new FordelRefusjonTjeneste(new MatchBeregningsgrunnlagTjeneste(beregningsgrunnlagRepository)));

        lagreBehandling(repositoryProvider);
        refusjonOver6GPrMnd = testUtil.getGrunnbeløp(STP_OPPTJENING).intValue() * 6 / 12 + 1;
    }

    private void lagreBehandling(RepositoryProvider repositoryProvider) {
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    private void utførSteg(BeregningsgrunnlagInput input) {
        BehandleStegResultat resultat94 = doStegFastsettSkjæringstidspunkt(input);
        assertThat(resultat94.getAksjonspunktResultater()).isEmpty();
        assertThat(resultat94.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        BehandleStegResultat resultat98 = doStegKontrollerFaktaBeregning(input);
        assertThat(resultat98.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        BehandleStegResultat resultat100 = doStegForeslåBeregning(input);
        assertThat(resultat100.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        BehandleStegResultat resultat105 = doStegFordelBeregningsgrunnlag(input);
        assertThat(resultat105.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    private BehandleStegResultat doStegKontrollerFaktaBeregning(BeregningsgrunnlagInput input) {
        List<BeregningAksjonspunktResultat> aksjonspunkter = kontrollerFaktaBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(this::mapBeregningResultat).collect(Collectors.toList()));
    }

    private BehandleStegResultat doStegForeslåBeregning(BeregningsgrunnlagInput input) {
        List<BeregningAksjonspunktResultat> aksjonspunkter = beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(input).getAksjonspunkter();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(this::mapBeregningResultat).collect(Collectors.toList()));
    }

    private BehandleStegResultat doStegFordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(input);
        List<BeregningAksjonspunktResultat> aksjonspunkter = utledAksjonspunkterForFordelBeregningsgrunnlag(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(this::mapBeregningResultat).collect(Collectors.toList()));
    }

    private List<BeregningAksjonspunktResultat> utledAksjonspunkterForFordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.utledAksjonspunkterForFordelBeregningsgrunnlag(input);
    }

    private List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        return beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
    }

    private Optional<FordelingDto> lagFaktaOmFordelingDto(BeregningsgrunnlagInput input) {
        return faktaOmFordelingDtoTjeneste.lagDto(input);
    }

    private void lagFordelBeregningsgrunnlagDto(BeregningsgrunnlagInput input,
                                                FordelingDto fordelingDto) {
        fordelBeregningsgrunnlagDtoTjeneste.lagDto(input, fordelingDto);
    }

    private BehandleStegResultat doStegFastsettSkjæringstidspunkt(BeregningsgrunnlagInput input) {
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater.stream().map(this::mapBeregningResultat).collect(Collectors.toList()));
    }

    @Test
    public void lag_fakta_om_beregning_med_fordeling_dto() {
        // Arrange
        String orgnr = ARBEIDSGIVER_ORGNR;
        String orgnr2 = "974761076";
        Arbeidsgiver arbeidsgiverGradering = Arbeidsgiver.virksomhet(orgnr);
        var internArbId1 = InternArbeidsforholdRef.nyRef();
        var internArbId2 = InternArbeidsforholdRef.nyRef();
        var eksternArbId1 = EksternArbeidsforholdRef.ref("ID1");
        var eksternArbId2 = EksternArbeidsforholdRef.ref("ID2");

        LocalDate graderingFom = STP_OPPTJENING.plusWeeks(9);
        LocalDate graderingTom = STP_OPPTJENING.plusWeeks(18);
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 20)
            .build());

        Periode opptjeningPeriode = new Periode(STP_OPPTJENING.minusWeeks(10), STP_OPPTJENING.minusDays(1));
        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr, internArbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2, internArbId2);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, internArbId2,
            STP_OPPTJENING, Collections.emptyList(), refusjonOver6GPrMnd, TIDENES_ENDE, INNSENDINGSTIDSPUNKT.get());

        Periode arbeidsperiode = new Periode(STP_OPPTJENING.minusWeeks(10), TIDENES_ENDE);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId1, eksternArbId1,
            arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr), INNTEKT_PR_MND);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId2, eksternArbId2,
            arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr2), INNTEKT_PR_MND);

        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(im1)
                .build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, aktivitetGradering, foreldrepengerGrunnlag);

        // Act
        utførSteg(input);

        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId());
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlagEntitet.orElseThrow());
        Optional<FordelingDto> dtoOpt = lagFaktaOmFordelingDto(newInput);

        // Assert
        assertThat(dtoOpt).isPresent();
        FordelingDto fordelingDto = dtoOpt.orElseThrow();

        // Act
        Optional<FordelingDto> dtoEtterFordelingOpt = bekreftFordelBeregningsgrunnlag(fordelingDto, newInput);

        // Assert
        assertEtterFordeling(dtoEtterFordelingOpt);
    }

    @Test
    public void lag_fordeling_dto_med_riktige_arbeidsprosenter_for_andel_med_sammenhengende_i_samme_periode_gradering() {
        String orgnr = ARBEIDSGIVER_ORGNR;
        String orgnr2 = "974761076";
        var internArbId1 = InternArbeidsforholdRef.nyRef();
        var internArbId2 = InternArbeidsforholdRef.nyRef();
        var eksternArbId1 = EksternArbeidsforholdRef.ref("ID1");
        var eksternArbId2 = EksternArbeidsforholdRef.ref("ID2");
        Arbeidsgiver arbeidsgiverGradering = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(ARBEIDSGIVER_ORGNR);
        Arbeidsgiver arbeidsgiverAnnen = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr2);
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);

        Periode arbeidsperiode = new Periode(STP_OPPTJENING.minusWeeks(10), TIDENES_ENDE);
        Periode opptjeningPeriode = new Periode(STP_OPPTJENING.minusWeeks(10), STP_OPPTJENING.minusDays(1));

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(STP_OPPTJENING, STP_OPPTJENING.plusWeeks(1), 20)
            .medGradering(STP_OPPTJENING.plusWeeks(1).plusDays(1), STP_OPPTJENING.plusWeeks(2), 30)
            .medGradering(STP_OPPTJENING.plusWeeks(2).plusDays(1), STP_OPPTJENING.plusWeeks(3), 40)
            .medGradering(STP_OPPTJENING.plusWeeks(3).plusDays(1), STP_OPPTJENING.plusWeeks(4), 50)
            .medGradering(STP_OPPTJENING.plusWeeks(4).plusDays(1), STP_OPPTJENING.plusWeeks(5), 60)
            .build());

        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr, internArbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2, internArbId2);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        // Arrange
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId1, eksternArbId1,
            arbeidsgiverGradering, INNTEKT_PR_MND);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId2, eksternArbId2,
            arbeidsgiverAnnen, INNTEKT_PR_MND);

        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, ARBEIDSGIVER_ORGNR, internArbId1, STP_OPPTJENING, INNSENDINGSTIDSPUNKT.get());
        var im2 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, internArbId2,
            STP_OPPTJENING, Collections.emptyList(), refusjonOver6GPrMnd, TIDENES_ENDE, INNSENDINGSTIDSPUNKT.get());
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(im1, im2)
                .build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, aktivitetGradering, foreldrepengerGrunnlag);

        // Act
        utførSteg(input);

        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId());
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlagEntitet.orElseThrow());
        Optional<FordelingDto> dtoOpt = lagFaktaOmFordelingDto(newInput);

        // Assert
        assertThat(dtoOpt).isPresent();
        FordelingDto fordelingDto = dtoOpt.orElseThrow();
        assertThat(fordelingDto.getFordelBeregningsgrunnlag().getFordelBeregningsgrunnlagPerioder()).hasSize(6);
        assertThat(fordelingDto.getFordelBeregningsgrunnlag().getFordelBeregningsgrunnlagPerioder().get(1).getFordelBeregningsgrunnlagAndeler())
            .hasSize(2);

        // Act
        Optional<FordelingDto> dtoEtterFordelingOpt = bekreftFordelBeregningsgrunnlag(fordelingDto, newInput);

        // Assert
        assertEtterFordeling(dtoEtterFordelingOpt);
    }

    @Test
    public void lag_fordeling_dto_med_riktige_arbeidsprosenter_for_andel_uten_sammenhengende_gradering_i_samme_periode() {
        // Arrange
        var internArbId1 = InternArbeidsforholdRef.nyRef();
        var internArbId2 = InternArbeidsforholdRef.nyRef();
        var eksternArbId1 = EksternArbeidsforholdRef.ref("ID1");
        var eksternArbId2 = EksternArbeidsforholdRef.ref("ID2");
        String orgnr = ARBEIDSGIVER_ORGNR;
        String orgnr2 = "974761076";
        Arbeidsgiver arbeidsgiverGradering = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr);
        Arbeidsgiver arbeidsgiverAnnen = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr2);
        int refusjonBeløp = testUtil.getGrunnbeløp(STP_OPPTJENING).intValue() * 6 / 12 + 1;

        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);

        Periode arbeidsperiode = new Periode(STP_OPPTJENING.minusWeeks(10), TIDENES_ENDE);
        Periode opptjeningPeriode = new Periode(STP_OPPTJENING.minusWeeks(10), STP_OPPTJENING.minusDays(1));

        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr, internArbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2, internArbId2);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(STP_OPPTJENING, STP_OPPTJENING.plusWeeks(1), 20)
            .medGradering(STP_OPPTJENING.plusWeeks(1).plusDays(1), STP_OPPTJENING.plusWeeks(2), 30)
            .medGradering(STP_OPPTJENING.plusWeeks(2).plusDays(1), STP_OPPTJENING.plusWeeks(3), 40)
            .medGradering(STP_OPPTJENING.plusWeeks(3).plusDays(1), STP_OPPTJENING.plusWeeks(4), 50)
            .medGradering(STP_OPPTJENING.plusWeeks(4).plusDays(1), STP_OPPTJENING.plusWeeks(5), 60)
            .build());

        // Arrange
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId1, eksternArbId1,  arbeidsgiverGradering,
            INNTEKT_PR_MND);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull(), internArbId2, eksternArbId2, arbeidsgiverAnnen,
            INNTEKT_PR_MND);

        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr, internArbId1, STP_OPPTJENING, INNSENDINGSTIDSPUNKT.get());
        var im2 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, internArbId2,
            STP_OPPTJENING, Collections.emptyList(), refusjonBeløp, TIDENES_ENDE,
            INNSENDINGSTIDSPUNKT.get());
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(im1, im2)
                .build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, aktivitetGradering, foreldrepengerGrunnlag);


        // Act
        utførSteg(input);

        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getId());
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlagEntitet.orElseThrow());
        Optional<FordelingDto> dtoOpt = lagFaktaOmFordelingDto(newInput);

        // Assert
        assertThat(dtoOpt).isPresent();
        FordelingDto fordelingDto = dtoOpt.orElseThrow();
        assertThat(fordelingDto.getFordelBeregningsgrunnlag().getFordelBeregningsgrunnlagPerioder()).hasSize(6);
        assertThat(fordelingDto.getFordelBeregningsgrunnlag().getFordelBeregningsgrunnlagPerioder().get(1).getFordelBeregningsgrunnlagAndeler())
            .hasSize(2);

        // Act
        Optional<FordelingDto> dtoEtterFordelingOpt = bekreftFordelBeregningsgrunnlag(fordelingDto, newInput);

        // Assert
        assertEtterFordeling(dtoEtterFordelingOpt);
    }

    @Test
    public void lag_fakta_om_beregning_for_gammel_andel_med_gradering_ingen_refusjon_avkortet_lik_0() {
        String orgnr1 = ARBEIDSGIVER_ORGNR;
        var internArbRef = InternArbeidsforholdRef.namedRef("B");
        var eksternArbRef = EksternArbeidsforholdRef.ref("ID1");
        String orgnr2 = "974761076";
        Arbeidsgiver arbeidsgiverGradering = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr1);
        Arbeidsgiver arbeidsgiverAnnen = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr2);
        Periode arbeidsperiode = new Periode(STP_OPPTJENING.minusWeeks(10), STP_OPPTJENING);
        LocalDate graderingFom = STP_OPPTJENING.plusWeeks(9);
        LocalDate graderingTom = STP_OPPTJENING.plusWeeks(18);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 20)
            .build());

        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, arbeidsperiode, orgnr1, null);
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, arbeidsperiode, orgnr2, internArbRef);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        // Arrange
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode, (InternArbeidsforholdRef)null, arbeidsgiverGradering, INNTEKT_PR_MND);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode, internArbRef, eksternArbRef, arbeidsgiverAnnen, INNTEKT_PR_MND);

        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, internArbRef, STP_OPPTJENING, 60000, INNSENDINGSTIDSPUNKT.get());

        leggTilAktivitet(internArbRef, orgnr2, arbeidsperiode.getFom(), arbeidsperiode.getTomOrNull());

        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(im1)
                .build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, aktivitetGradering, foreldrepengerGrunnlag);


        // Act
        utførSteg(input);

        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId());
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlagEntitet.orElseThrow());
        Optional<FordelingDto> dtoOpt = lagFaktaOmFordelingDto(newInput);

        // Assert
        FordelingDto fordelingDto = dtoOpt.orElseThrow();
        FordelBeregningsgrunnlagDto fordelingBg = fordelingDto.getFordelBeregningsgrunnlag();
        assertThat(fordelingBg.getFordelBeregningsgrunnlagPerioder()).hasSize(3);
        List<FordelBeregningsgrunnlagArbeidsforholdDto> arbeidsforholdTilFordeling = fordelingBg.getArbeidsforholdTilFordeling();
        assertThat(arbeidsforholdTilFordeling).hasSize(1);
        FordelBeregningsgrunnlagArbeidsforholdDto fordelArbeidsforholdDto = arbeidsforholdTilFordeling.get(0);
        assertThat(fordelArbeidsforholdDto.getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(fordelArbeidsforholdDto.getPerioderMedGraderingEllerRefusjon().get(0).getFom()).isEqualTo(graderingFom);

        // Act
        Optional<FordelingDto> dtoEtterFordelingOpt = bekreftFordelBeregningsgrunnlag(fordelingDto, newInput);

        // Assert
        assertEtterFordeling(dtoEtterFordelingOpt);
    }

    @Test
    public void skal_sette_skal_kunne_endre_refusjon_på_riktig_periode() {
        // Arrange
        String orgnr1 = ARBEIDSGIVER_ORGNR;
        String orgnr2 = "974761076";
        InternArbeidsforholdRef arbId1 = InternArbeidsforholdRef.nyRef();
        InternArbeidsforholdRef arbId2 = InternArbeidsforholdRef.nyRef();

        LocalDate graderingFom = STP_OPPTJENING.plusWeeks(9);
        LocalDate graderingTom = STP_OPPTJENING.plusWeeks(18);
        Arbeidsgiver arbeidsgiverGradering = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr1);
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 20)
            .build());

        Periode opptjeningPeriode = new Periode(STP_OPPTJENING.minusMonths(10), STP_OPPTJENING.minusDays(1));
        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr1, null);
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, orgnr2, null);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        Periode arbeidsperiode = new Periode(STP_OPPTJENING.minusMonths(10), TIDENES_ENDE);
        Arbeidsgiver arbeidsgiverAnnen = arbeidsgiverTestUtil.forArbeidsgiverVirksomhet(orgnr2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode, arbId1, arbeidsgiverGradering, INNTEKT_PR_MND);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, STP_OPPTJENING, arbeidsperiode, arbId2, arbeidsgiverAnnen, INNTEKT_PR_MND);

        Integer inntekt = 10000;
        var im1 = inntektsmeldingUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, arbId2, STP_OPPTJENING, Collections.emptyList(), refusjonOver6GPrMnd, inntekt,
            INNSENDINGSTIDSPUNKT.get());
        BehandlingReferanse ref = lagReferanse(behandlingReferanse);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(im1)
                .build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, aktivitetGradering, foreldrepengerGrunnlag);


        // Act
        utførSteg(input);

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(3);
        Optional<BeregningsgrunnlagPeriode> graderingPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(periode -> periode.getPeriodeÅrsaker().contains(PeriodeÅrsak.GRADERING)).findFirst();
        assertThat(graderingPeriode).isPresent();
        assertThat(graderingPeriode.orElseThrow().getPeriode().getFomDato()).isEqualTo(graderingFom);
        assertThat(graderingPeriode.orElseThrow().getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BeregningsgrunnlagPrStatusOgAndel> andelMedGradering = graderingPeriode.orElseThrow().getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> Objects.equals(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdOrgnr).orElse(null), orgnr1))
            .findFirst();
        assertThat(andelMedGradering).isPresent();
        assertThat(andelMedGradering.orElseThrow().getAvkortetPrÅr()).isNull();

        // Act
        var beregningAktiviter = beregningAktivitetBuilder.build();
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktiviter)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlag);

        FordelingDto fordelingDto = new FordelingDto();
        lagFordelBeregningsgrunnlagDto(newInput, fordelingDto);

        // Assert
        FordelBeregningsgrunnlagDto fordelingBgDto = fordelingDto.getFordelBeregningsgrunnlag();
        assertThat(fordelingBgDto.getFordelBeregningsgrunnlagPerioder()).hasSize(3);
        Optional<FordelBeregningsgrunnlagPeriodeDto> periodeUtenGraderingOpt = fordelingBgDto.getFordelBeregningsgrunnlagPerioder().stream()
            .filter(periode -> periode.getFom().equals(STP_OPPTJENING)).findFirst();
        assertThat(periodeUtenGraderingOpt).hasValueSatisfying(periodeUtenGradering -> assertThat(periodeUtenGradering.isSkalKunneEndreRefusjon()).isFalse());

        Optional<FordelBeregningsgrunnlagPeriodeDto> periodeMedGraderingOpt = fordelingBgDto.getFordelBeregningsgrunnlagPerioder().stream()
            .filter(periode -> periode.getFom().equals(graderingFom)).findFirst();
        assertThat(periodeMedGraderingOpt).hasValueSatisfying(periodeMedGradering -> assertThat(periodeMedGradering.isSkalKunneEndreRefusjon()).isTrue());
    }

    private Optional<FordelingDto> bekreftFordelBeregningsgrunnlag(FordelingDto fordelingDto, BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FordelBeregningsgrunnlagDto dto = lagOppdatererDto(fordelingDto);
        fordelBeregningsgrunnlagHåndterer.håndter(dto, behandlingReferanse.getId());
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId());
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlag.orElseThrow());
        return lagFaktaOmFordelingDto(newInput);
    }

    private void assertEtterFordeling(Optional<FordelingDto> dtoEtterFordeling) {
        assertThat(dtoEtterFordeling).isPresent();
        FordelingDto faktaOmFordelingDto = dtoEtterFordeling.orElseThrow();
        FordelBeregningsgrunnlagDto fordelBeregningsgrunnlag = faktaOmFordelingDto.getFordelBeregningsgrunnlag();
        fordelBeregningsgrunnlag.getFordelBeregningsgrunnlagPerioder().stream()
            .filter(FordelBeregningsgrunnlagPeriodeDto::isHarPeriodeAarsakGraderingEllerRefusjon)
            .forEach(periode -> periode.getFordelBeregningsgrunnlagAndeler()
                .forEach(andel -> assertFastsatteVerdier(periode, andel)));
    }

    private void assertFastsatteVerdier(FordelBeregningsgrunnlagPeriodeDto periode, FordelBeregningsgrunnlagAndelDto andel) {
        if (skalKunneEndreRefusjon(periode, andel)) {
            assertThat(andel.getRefusjonskravPrAar().intValue()).isEqualTo(FASTSATT_REFUSJON*12);
        }
        assertThat(andel.getFordeltPrAar().intValue()).isEqualTo(FASTSATT_BELØP*12);
        assertThat(andel.getInntektskategori()).isEqualTo(FASTSATT_INNTEKTSKATEGORI);
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FordelBeregningsgrunnlagDto lagOppdatererDto(FordelingDto fordelingDto) {
        List<FastsettBeregningsgrunnlagPeriodeDto> perioder = lagFastsettBeregningsgrunnlag(fordelingDto);
        return new no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FordelBeregningsgrunnlagDto(perioder, "begrunnelse");
    }

    private List<FastsettBeregningsgrunnlagPeriodeDto> lagFastsettBeregningsgrunnlag(FordelingDto dto) {
        FordelBeregningsgrunnlagDto fordelingBeregningsgrunnlag = dto.getFordelBeregningsgrunnlag();
        return lagFastsettPerioder(fordelingBeregningsgrunnlag);
    }

    private List<FastsettBeregningsgrunnlagPeriodeDto> lagFastsettPerioder(FordelBeregningsgrunnlagDto fordelBeregningsgrunnlag) {
        return fordelBeregningsgrunnlag.getFordelBeregningsgrunnlagPerioder().stream()
            .filter(FordelBeregningsgrunnlagPeriodeDto::isHarPeriodeAarsakGraderingEllerRefusjon)
            .map(periode -> {
                List<FastsettBeregningsgrunnlagAndelDto> andeler = lagFastsettAndelerList(periode);
                return new FastsettBeregningsgrunnlagPeriodeDto(andeler, periode.getFom(), periode.getFom());
            }).collect(toList());
    }

    private List<FastsettBeregningsgrunnlagAndelDto> lagFastsettAndelerList(FordelBeregningsgrunnlagPeriodeDto periode) {
        return periode.getFordelBeregningsgrunnlagAndeler().stream().map(andel -> {
            RedigerbarAndelDto andelDto = lagRedigerbarAndelDto(andel);
            FastsatteVerdierDto fastsattBeløp = lagFastsatteVerdier(periode, andel);
            return new FastsettBeregningsgrunnlagAndelDto(andelDto, fastsattBeløp, andel.getInntektskategori(), andel.getRefusjonskravPrAar().intValue(), null);
        }).collect(toList());
    }

    private FastsatteVerdierDto lagFastsatteVerdier(FordelBeregningsgrunnlagPeriodeDto periode, FordelBeregningsgrunnlagAndelDto andel) {
        return new FastsatteVerdierDto(skalKunneEndreRefusjon(periode, andel) ? FASTSATT_REFUSJON : null, FASTSATT_BELØP, FASTSATT_INNTEKTSKATEGORI, null);
    }

    private RedigerbarAndelDto lagRedigerbarAndelDto(FordelBeregningsgrunnlagAndelDto andel) {
        return new RedigerbarAndelDto(false,
            andel.getArbeidsforhold() != null ? andel.getArbeidsforhold().getArbeidsgiverId() : null, andel.getArbeidsforhold() != null ? andel.getArbeidsforhold().getArbeidsforholdId() : null,
            andel.getAndelsnr(), andel.getLagtTilAvSaksbehandler(), andel.getAktivitetStatus(), OpptjeningAktivitetType.ARBEID);
    }

    private boolean skalKunneEndreRefusjon(FordelBeregningsgrunnlagPeriodeDto periode, FordelBeregningsgrunnlagAndelDto andel) {
        return periode.isSkalKunneEndreRefusjon() && !andel.getAktivitetStatus().equals(AktivitetStatus.BRUKERS_ANDEL);
    }

    private BehandlingReferanse lagReferanse(BehandlingReferanse behandlingReferanse) {
        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(STP_OPPTJENING)
            .medSkjæringstidspunktOpptjening(STP_OPPTJENING)
            .medSkjæringstidspunktBeregning(STP_OPPTJENING)
            .medFørsteUttaksdato(STP_OPPTJENING.plusDays(1))
            .build();
        return behandlingReferanse.medSkjæringstidspunkt(stp);
    }

    private void leggTilAktivitet(InternArbeidsforholdRef ref, String orgnr, LocalDate fom, LocalDate tom) {
        beregningAktivitetBuilder.leggTilAktivitet(BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom == null ? TIDENES_ENDE : tom))
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdRef(ref).build());
    }

    private AksjonspunktResultat mapBeregningResultat(BeregningAksjonspunktResultat beregningResultat) {
        if (beregningResultat.harFrist()) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()),
                Venteårsak.fraKode(beregningResultat.getVenteårsak().getKode()),
                beregningResultat.getVentefrist());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()));
    }
}
