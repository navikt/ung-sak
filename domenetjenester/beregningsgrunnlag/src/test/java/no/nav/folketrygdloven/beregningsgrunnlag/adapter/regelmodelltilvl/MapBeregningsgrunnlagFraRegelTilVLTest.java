package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_DAYS_10;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_DAYS_20;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_DAYS_5;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_YEARS_1;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_YEARS_2;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_YEARS_3;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildRegelBGPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildRegelBeregningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildRegelSammenligningsG;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGAktivitetStatus;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPStatus;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPStatusForSN;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPeriode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfig;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ResultatBeregningType;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class MapBeregningsgrunnlagFraRegelTilVLTest {

    private static final String ORGNR = "974761076";

    private BehandlingReferanse behandlingReferanse;
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    public final ExpectedException expectedException = ExpectedException.none();
    @Inject
    private MapBGSkjæringstidspunktOgStatuserFraRegelTilVL mapBeregningsgrunnlagFraRegelTilVL;
    @Inject
    private BeregningIAYTestUtil iayTestUtil;

    @Before
    public void setup() {
        behandlingReferanse = TestScenarioBuilder.nyttScenario().lagre(repositoryProvider);

    }

    @Test
    public void testMappingBGForSN() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = buildVLBG();

        List<RegelResultat> regelresultater = List.of(new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing"));
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mappedBG = new MapBeregningsgrunnlagFraRegelTilVL()
            .mapFastsettBeregningsgrunnlag(buildRegelBGForSN(), regelresultater, vlBG);

        assertThat(mappedBG).isNotSameAs(vlBG);
        assertThat(mappedBG.getSammenligningsgrunnlag().getSammenligningsperiodeFom()).isEqualTo(MINUS_YEARS_1);
        assertThat(mappedBG.getSammenligningsgrunnlag().getSammenligningsperiodeTom()).isEqualTo(MINUS_DAYS_20);
        assertThat(mappedBG.getSammenligningsgrunnlag().getRapportertPrÅr().doubleValue()).isEqualTo(42.0);
        assertVLSammenligningsgrunnlagPrStatus(mappedBG.getSammenligningsgrunnlagPrStatusListe().get(0), SammenligningsgrunnlagType.SAMMENLIGNING_SN);

        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGP = mappedBG.getBeregningsgrunnlagPerioder()
            .get(0);
        assertThat(vlBGP.getBruttoPrÅr().doubleValue()).isEqualTo(400000.42, within(0.01));
        assertThat(vlBGP.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(vlBGPStatus.getAktivitetStatus())
            .isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertVLBGPStatusSN(vlBGPStatus);
    }

    @Test
    public void testMappingBGForArbeidstaker() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = buildVLBGForATOgFL();
        List<RegelResultat> regelresultater = List.of(new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing"));

        Beregningsgrunnlag resultatGrunnlag = buildRegelBGForAT();
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mappedBG = new MapBeregningsgrunnlagFraRegelTilVL()
            .mapForeslåBeregningsgrunnlag(resultatGrunnlag, regelresultater, vlBG);

        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGP = mappedBG.getBeregningsgrunnlagPerioder()
            .get(0);

        assertThat(vlBGP.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus1 = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertVLBGPStatusAT(vlBGPStatus1);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus2 = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(1);
        assertVLBGPStatusFL(vlBGPStatus2);
        assertVLSammenligningsgrunnlagPrStatus(mappedBG.getSammenligningsgrunnlagPrStatusListe().get(0), SammenligningsgrunnlagType.SAMMENLIGNING_AT);
    }

    @Test
    public void testMappingBGForATFLogSN() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = buildVLBGForATFLogSN();
        List<RegelResultat> regelresultater = List.of(new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing"));
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mappedBG = new MapBeregningsgrunnlagFraRegelTilVL()
            .mapFastsettBeregningsgrunnlag(buildRegelBGForATFLogSN(), regelresultater, vlBG);

        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGP = mappedBG.getBeregningsgrunnlagPerioder()
            .get(0);

        assertThat(vlBGP.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(vlBGPStatus.getAktivitetStatus())
            .isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertVLBGPStatusSN(vlBGPStatus);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus1 = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(1);
        assertThat(vlBGPStatus1.getAktivitetStatus())
            .isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER);
        assertVLBGPStatusAT(vlBGPStatus1);
        final BeregningsgrunnlagPrStatusOgAndel vlBGPStatus2 = vlBGP.getBeregningsgrunnlagPrStatusOgAndelList().get(2);
        assertThat(vlBGPStatus2.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.FRILANSER);
        assertVLBGPStatusFL(vlBGPStatus2);
    }

    @Test
    public void skal_mappe_beregningsgrunnlag_når_arbeidsgiver_er_privatperson() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2018, 1, 1);
        LocalDate førsteUttaksdag = skjæringstidspunkt.plusWeeks(2);
        AktørId aktørId = AktørId.dummy();
        AktivitetStatusModell regelmodell = lagRegelModell(skjæringstidspunkt, Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(aktørId.getId()));
        String inputSkjæringstidspunkt = toJson(regelmodell);
        RegelResultat regelResultat = new RegelResultat(ResultatBeregningType.BEREGNET, inputSkjæringstidspunkt, "sporing");
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, skjæringstidspunkt, skjæringstidspunkt.minusYears(1), skjæringstidspunkt, null,
            Arbeidsgiver.person(aktørId));

        // Act
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag = mapBeregningsgrunnlagFraRegelTilVL
            .mapForSkjæringstidspunktOgStatuser(behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdag).build()), regelmodell,
                List.of(regelResultat, regelResultat), iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        // Andel asserts
        assertThat(andel.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER);
        // Arbeidsforhold asserts
        assertThat(andel.getBgAndelArbeidsforhold()).isPresent();
        BGAndelArbeidsforhold bga = andel.getBgAndelArbeidsforhold().get();
        assertThat(bga.getArbeidsgiver().getErVirksomhet()).isFalse();
        assertThat(bga.getArbeidsgiver().getIdentifikator()).isEqualTo(aktørId.getId());
    }

    @Test
    public void skal_mappe_beregningsgrunnlag_når_arbeidsgiver_er_virksomhet() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2018, 1, 1);
        LocalDate førsteUttaksdag = skjæringstidspunkt.plusWeeks(2);
        RegelResultat regelResultat = new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing");
        AktivitetStatusModell regelmodell = lagRegelModell(skjæringstidspunkt, Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, skjæringstidspunkt, skjæringstidspunkt.minusYears(1), skjæringstidspunkt, null,
            Arbeidsgiver.virksomhet(ORGNR));

        // Act
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag = mapBeregningsgrunnlagFraRegelTilVL
            .mapForSkjæringstidspunktOgStatuser(behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdag).build()), regelmodell,
                List.of(regelResultat, regelResultat), iayTestUtil.getIayTjeneste().hentGrunnlag(behandlingReferanse.getBehandlingId()));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        // Andel asserts
        assertThat(andel.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER);
        // Arbeidsforhold asserts
        assertThat(andel.getBgAndelArbeidsforhold()).isPresent();
        BGAndelArbeidsforhold bga = andel.getBgAndelArbeidsforhold().get();
        assertThat(bga.getArbeidsgiver().getErVirksomhet()).isTrue();
        assertThat(bga.getArbeidsgiver().getIdentifikator()).isEqualTo(ORGNR);
    }

    @Test
    public void skal_mappe_beregningsgrunnlag_for_næring() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2018, 1, 1);
        LocalDate førsteUttaksdag = skjæringstidspunkt.plusWeeks(2);
        RegelResultat regelResultat = new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing");
        AktivitetStatusModell regelmodell = lagRegelModellSN(skjæringstidspunkt);

        // Act
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag = mapBeregningsgrunnlagFraRegelTilVL
            .mapForSkjæringstidspunktOgStatuser(behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder().medFørsteUttaksdato(førsteUttaksdag).build()), regelmodell,
                List.of(regelResultat, regelResultat), iayTestUtil.getIayTjeneste().finnGrunnlag(behandlingReferanse.getBehandlingId()).orElse(InntektArbeidYtelseGrunnlagBuilder.nytt().build()));

        // Assert
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(andel.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andel.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.NÆRING);
    }

    @Test
    public void skalTesteMappingAvRegelsporingOgInputForFastsettingAvBeregningsgrunnlag() {
        //Arrange
        final var vlBG = buildVLBGForATOgFL();
        RegelResultat regelResultat = new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing")
            .medRegelsporingFinnGrenseverdi("input3", "sporing3");
        Beregningsgrunnlag resultatGrunnlag = buildRegelBGForAT();

        //Act
        final var mappedBG = new MapBeregningsgrunnlagFraRegelTilVL().mapFastsettBeregningsgrunnlag(resultatGrunnlag, List.of(regelResultat), vlBG);

        //Assert
        final var vlPeriode = mappedBG.getBeregningsgrunnlagPerioder().get(0);
        assertThat(vlPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        assertThat(vlPeriode.getRegelEvalueringFastsett()).isEqualTo("sporing");
        assertThat(vlPeriode.getRegelInputFastsett()).isEqualTo("input");
        assertThat(vlPeriode.getRegelEvalueringFinnGrenseverdi()).isEqualTo("sporing3");
        assertThat(vlPeriode.getRegelInputFinnGrenseverdi()).isEqualTo("input3");
    }

    @Test
    public void skalSetteRiktigSammenligningsgrunnlagPrStatusNårBeregningsgrunnlagInneholderSammenlingningsgrunnlagForFL() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = buildVLBGForATOgFL();
        List<RegelResultat> regelresultater = List.of(new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing"));

        Beregningsgrunnlag resultatGrunnlag = buildRegelBGForFL();
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mappedBG = new MapBeregningsgrunnlagFraRegelTilVL()
            .mapForeslåBeregningsgrunnlag(resultatGrunnlag, regelresultater, vlBG);

        assertVLSammenligningsgrunnlagPrStatus(mappedBG.getSammenligningsgrunnlagPrStatusListe().get(0), SammenligningsgrunnlagType.SAMMENLIGNING_FL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void skalKasteExceptionNårManPrøverÅMappeTilSammenligningsgrunnlagTypeSomIkkeFinnes() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = buildVLBGForATOgFL();
        List<RegelResultat> regelresultater = List.of(new RegelResultat(ResultatBeregningType.BEREGNET, "input", "sporing"));

        Beregningsgrunnlag resultatGrunnlag = buildRegelBGForFL();
        Beregningsgrunnlag.builder(resultatGrunnlag).medSammenligningsgrunnlagPrStatus(AktivitetStatus.UDEFINERT, buildRegelSammenligningsG()).build();

        new MapBeregningsgrunnlagFraRegelTilVL() .mapForeslåBeregningsgrunnlag(resultatGrunnlag, regelresultater, vlBG);
    }

    private void assertVLSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlag, SammenligningsgrunnlagType sammenligningsgrunnlagType){
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeFom()).isEqualTo(MINUS_YEARS_1);
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeTom()).isEqualTo(MINUS_DAYS_20);
        assertThat(sammenligningsgrunnlag.getRapportertPrÅr().doubleValue()).isEqualTo(42.0);
        assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagType()).isEqualTo(sammenligningsgrunnlagType);
    }

    private void assertVLBGPStatusSN(BeregningsgrunnlagPrStatusOgAndel vlBGPStatus) {
        assertThat(vlBGPStatus.getAktivitetStatus())
            .isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(vlBGPStatus.getInntektskategori()).isEqualTo(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(vlBGPStatus.getBeregnetPrÅr().doubleValue()).isEqualTo(400000.42, within(0.01));
        assertThat(vlBGPStatus.getBruttoPrÅr().doubleValue()).isEqualTo(400000.42, within(0.01));
        assertThat(vlBGPStatus.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null)).isNull();
        assertThat(vlBGPStatus.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();
        assertThat(vlBGPStatus.getAvkortetPrÅr().doubleValue()).isEqualTo(789.789, within(0.01));
        assertThat(vlBGPStatus.getRedusertPrÅr().doubleValue()).isEqualTo(901.901, within(0.01));
        assertThat(vlBGPStatus.getDagsatsArbeidsgiver()).isEqualTo(0L);
    }

    private void assertVLBGPStatusFL(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.FRILANSER);
        assertThat(bgpsa.getInntektskategori()).isEqualTo(Inntektskategori.FRILANSER);
        assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)).isEmpty();
        assertThat(bgpsa.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
            .as("gjelderSpesifiktArbeidsforhold").isFalse();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(456.456, within(0.01));
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(456.456, within(0.01));
        assertThat(bgpsa.getBgAndelArbeidsforhold()).isEmpty();
        assertThat(bgpsa.getAvkortetPrÅr().doubleValue()).isEqualTo(34.34, within(0.01));
        assertThat(bgpsa.getRedusertPrÅr().doubleValue()).isEqualTo(65.65, within(0.01));
        assertThat(bgpsa.getDagsatsArbeidsgiver()).isEqualTo(5L);
    }

    private void assertVLBGPStatusAT(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER);
        assertThat(bgpsa.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
            .hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver.getOrgnr()).isEqualTo(ORGNR));
        assertThat(bgpsa.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
            .as("gjelderSpesifiktArbeidsforhold").isFalse();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(123.123, within(0.01));
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(123.123, within(0.01));
        assertThat(bgpsa.getMaksimalRefusjonPrÅr().doubleValue()).isEqualTo(123.123, within(0.01));
        assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr).get().doubleValue()).isEqualTo(87.87,
            within(0.01));
        assertThat(bgpsa.getAvkortetPrÅr().doubleValue()).isEqualTo(57.57, within(0.01));
        assertThat(bgpsa.getRedusertPrÅr().doubleValue()).isEqualTo(89.89, within(0.01));
        assertThat(bgpsa.getDagsatsArbeidsgiver()).isEqualTo(10L);
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet buildVLBG() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = RegelMapperTestDataHelper
            .buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(vlBG);
        buildVLBGPStatusForSN(buildVLBGPeriode(vlBG));
        return vlBG;
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet buildVLBGForATOgFL() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = RegelMapperTestDataHelper
            .buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(vlBG);
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode = buildVLBGPeriode(vlBG);
        buildVLBGPStatus(vlBGPeriode, no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_2,
            MINUS_YEARS_1, Arbeidsgiver.virksomhet(ORGNR), OpptjeningAktivitetType.ARBEID);
        buildVLBGPStatus(vlBGPeriode, no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, MINUS_YEARS_3, MINUS_YEARS_2);
        return vlBG;
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet buildVLBGForATFLogSN() {
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet vlBG = RegelMapperTestDataHelper
            .buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(vlBG);
        final no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode = buildVLBGPeriode(vlBG);
        buildVLBGPStatusForSN(vlBGPeriode);
        buildVLBGPStatus(vlBGPeriode, no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_2,
            MINUS_YEARS_1, Arbeidsgiver.virksomhet(ORGNR), OpptjeningAktivitetType.ARBEID);
        buildVLBGPStatus(vlBGPeriode, no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, MINUS_YEARS_3, MINUS_YEARS_2);
        return vlBG;
    }

    private Beregningsgrunnlag buildRegelBGForSN() {
        final Beregningsgrunnlag regelBG = buildRegelBeregningsgrunnlag(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN,
            no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
            BeregningsgrunnlagHjemmel.HJEMMEL_BARE_SELVSTENDIG);
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlag(buildRegelSammenligningsG()).build();
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlagPrStatus(AktivitetStatus.SN, buildRegelSammenligningsG()).build();

        final BeregningsgrunnlagPeriode regelBGP = regelBG.getBeregningsgrunnlagPerioder().get(0);

        buildRegelBGPeriodeSN(regelBGP);
        return regelBG;
    }

    private Beregningsgrunnlag buildRegelBGForAT() {
        final Beregningsgrunnlag regelBG = buildRegelBeregningsgrunnlag(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL,
            null,
            BeregningsgrunnlagHjemmel.HJEMMEL_BARE_ARBEIDSTAKER);
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlag(buildRegelSammenligningsG()).build();
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlagPrStatus(AktivitetStatus.AT, buildRegelSammenligningsG()).build();

        final BeregningsgrunnlagPeriode regelBGP = regelBG.getBeregningsgrunnlagPerioder().get(0);

        buildRegelBGPStatusATFL(regelBGP, 1);
        return regelBG;
    }

    private Beregningsgrunnlag buildRegelBGForFL() {
        final Beregningsgrunnlag regelBG = buildRegelBeregningsgrunnlag(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL,
            null,
            BeregningsgrunnlagHjemmel.HJEMMEL_BARE_FRILANSER);
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlag(buildRegelSammenligningsG()).build();
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlagPrStatus(AktivitetStatus.FL, buildRegelSammenligningsG()).build();

        final BeregningsgrunnlagPeriode regelBGP = regelBG.getBeregningsgrunnlagPerioder().get(0);

        buildRegelBGPStatusATFL(regelBGP, 1);
        return regelBG;
    }

    private Beregningsgrunnlag buildRegelBGForATFLogSN() {
        final Beregningsgrunnlag regelBG = buildRegelBeregningsgrunnlag(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL_SN,
            null,
            BeregningsgrunnlagHjemmel.HJEMMEL_ARBEIDSTAKER_OG_SELVSTENDIG);
        Beregningsgrunnlag.builder(regelBG).medSammenligningsgrunnlag(buildRegelSammenligningsG()).build();

        final BeregningsgrunnlagPeriode regelBGP = regelBG.getBeregningsgrunnlagPerioder().get(0);

        buildRegelBGPeriodeSN(regelBGP);
        buildRegelBGPStatusATFL(regelBGP, 2);
        return regelBG;
    }

    private void buildRegelBGPeriodeSN(BeregningsgrunnlagPeriode regelBGP) {
        buildRegelBGPeriode(regelBGP, AktivitetStatus.SN, new Periode(MINUS_DAYS_10, MINUS_DAYS_5));
    }

    private void buildRegelBGPStatusATFL(BeregningsgrunnlagPeriode regelBGP, long andelNr) {
        final BeregningsgrunnlagPrStatus regelBGPStatus = buildRegelBGPeriode(regelBGP, AktivitetStatus.ATFL, new Periode(MINUS_YEARS_2, MINUS_YEARS_1));
        final BeregningsgrunnlagPrArbeidsforhold regelArbeidsforhold42 = BeregningsgrunnlagPrArbeidsforhold.builder()
            .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR))
            .medInntektskategori(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori.ARBEIDSTAKER)
            .medAndelNr(andelNr++)
            .medFordeltPrÅr(BigDecimal.valueOf(123.123))
            .medBeregnetPrÅr(BigDecimal.valueOf(123.123))
            .medMaksimalRefusjonPrÅr(BigDecimal.valueOf(123.123))
            .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(87.87))
            .medAvkortetPrÅr(BigDecimal.valueOf(57.57))
            .medRedusertPrÅr(BigDecimal.valueOf(89.89))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(2600.0))
            .build();

        final BeregningsgrunnlagPrArbeidsforhold regelArbeidsforhold66 = BeregningsgrunnlagPrArbeidsforhold.builder()
            .medArbeidsforhold(Arbeidsforhold.frilansArbeidsforhold())
            .medInntektskategori(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskategori.FRILANSER)
            .medAndelNr(andelNr)
            .medFordeltPrÅr(BigDecimal.valueOf(456.456))
            .medBeregnetPrÅr(BigDecimal.valueOf(456.456))
            .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(45.45))
            .medAvkortetPrÅr(BigDecimal.valueOf(34.34))
            .medRedusertPrÅr(BigDecimal.valueOf(65.65))
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(1300.0))
            .build();
        BeregningsgrunnlagPrStatus.builder(regelBGPStatus)
            .medArbeidsforhold(regelArbeidsforhold42)
            .medArbeidsforhold(regelArbeidsforhold66)
            .build();
    }

    private String toJson(AktivitetStatusModell grunnlag) {
        return JacksonJsonConfig.toJson(grunnlag, null);
    }

    private AktivitetStatusModell lagRegelModell(LocalDate skjæringstidspunkt, Arbeidsforhold arbeidsforhold) {
        AktivitetStatusModell regelmodell = new AktivitetStatusModell();
        regelmodell.setSkjæringstidspunktForBeregning(skjæringstidspunkt);
        regelmodell.setSkjæringstidspunktForOpptjening(skjæringstidspunkt);
        regelmodell.leggTilAktivitetStatus(AktivitetStatus.ATFL);
        var bgPrStatus = new no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, arbeidsforhold);
        regelmodell.leggTilBeregningsgrunnlagPrStatus(bgPrStatus);
        return regelmodell;
    }


    private AktivitetStatusModell lagRegelModellSN(LocalDate skjæringstidspunkt) {
        AktivitetStatusModell regelmodell = new AktivitetStatusModell();
        regelmodell.setSkjæringstidspunktForBeregning(skjæringstidspunkt);
        regelmodell.setSkjæringstidspunktForOpptjening(skjæringstidspunkt);
        regelmodell.leggTilAktivitetStatus(AktivitetStatus.SN);
        no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.BeregningsgrunnlagPrStatus bgPrStatus = new no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.BeregningsgrunnlagPrStatus(
            AktivitetStatus.SN);
        regelmodell.leggTilBeregningsgrunnlagPrStatus(bgPrStatus);
        return regelmodell;
    }
}
