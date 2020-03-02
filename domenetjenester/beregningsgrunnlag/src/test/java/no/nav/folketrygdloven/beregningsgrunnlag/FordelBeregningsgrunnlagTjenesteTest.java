package no.nav.folketrygdloven.beregningsgrunnlag;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLTest.GRUNNBELØP;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.konfig.Tid;

public class FordelBeregningsgrunnlagTjenesteTest {

    private static final String ORGNR1 = "995428563";
    private static final String ORGNR2 = "910909088";
    private static final String ORGNR3 = "973861778";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 4);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private final BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);
    private FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste;
    private List<BeregningAktivitetEntitet> aktiviteter = new ArrayList<>();
    private TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
    private BeregningAktivitetAggregatEntitet beregningAktivitetAggregat;
    private BehandlingReferanse behandlingReferanse;

    @Before
    public void oppsett() {
        MapBeregningsgrunnlagFraVLTilRegel mapBeregningsgrunnlagFraVLTilRegel = new MapBeregningsgrunnlagFraVLTilRegel( null, null);
        MapBeregningsgrunnlagFraRegelTilVL mapBeregningsgrunnlagFraRegelTilVL = new MapBeregningsgrunnlagFraRegelTilVL();
        fordelBeregningsgrunnlagTjeneste = new FordelBeregningsgrunnlagTjeneste(lagTjeneste(), mapBeregningsgrunnlagFraRegelTilVL, mapBeregningsgrunnlagFraVLTilRegel);
        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORGNR1, ORGNR2, ORGNR3));
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT);
        aktiviteter.forEach(builder::leggTilAktivitet);
        beregningAktivitetAggregat = builder.build();
        this.behandlingReferanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void skal_omfordele_når_refusjon_overstiger_beregningsgrunnlag_for_ein_andel() {
        // Arrange
        // Beregningsgrunnlag fra Foreslå
        BigDecimal beregnetPrÅr1 = BigDecimal.valueOf(120_000);
        Map<String, BigDecimal> orgnrsBeregnetMap = new HashMap<>();
        orgnrsBeregnetMap.put(ORGNR1, beregnetPrÅr1);
        BigDecimal beregnetPrÅr2 = BigDecimal.valueOf(180_000);
        orgnrsBeregnetMap.put(ORGNR2, beregnetPrÅr2);
        BigDecimal beregnetPrÅr3 = BigDecimal.valueOf(240_000);
        orgnrsBeregnetMap.put(ORGNR3, beregnetPrÅr3);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(orgnrsBeregnetMap, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        // Inntektsmelding
        BigDecimal inntektPrMnd1 = BigDecimal.valueOf(10_000);
        BigDecimal refusjonPrMnd1 = BigDecimal.valueOf(20_000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR1, SKJÆRINGSTIDSPUNKT, refusjonPrMnd1, inntektPrMnd1, SKJÆRINGSTIDSPUNKT.atStartOfDay());
        BigDecimal inntektPrMnd2 = BigDecimal.valueOf(15_000);
        BigDecimal refusjonPrMnd2 = BigDecimal.valueOf(15_000);
        var im2 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR2, SKJÆRINGSTIDSPUNKT, refusjonPrMnd2, inntektPrMnd2, SKJÆRINGSTIDSPUNKT.atStartOfDay());
        BigDecimal inntektPrMnd3 = BigDecimal.valueOf(20_000);
        BigDecimal refusjonPrMnd3 = BigDecimal.ZERO;
        var im3 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR3, SKJÆRINGSTIDSPUNKT, refusjonPrMnd3, inntektPrMnd3, SKJÆRINGSTIDSPUNKT.atStartOfDay());
        var inntektsmeldinger = List.of(im1, im2, im3);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlag);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fordelBeregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(input, beregningsgrunnlag);

        // Assert
        assertThat(nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().size()).isEqualTo(1);
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = periode.getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler.size()).isEqualTo(3);
        BeregningsgrunnlagPrStatusOgAndel andel1 = andeler.stream().filter(a -> a.getBgAndelArbeidsforhold().get().getArbeidsgiver().getIdentifikator().equals(ORGNR1)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andel2 = andeler.stream().filter(a -> a.getBgAndelArbeidsforhold().get().getArbeidsgiver().getIdentifikator().equals(ORGNR2)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel andel3 = andeler.stream().filter(a -> a.getBgAndelArbeidsforhold().get().getArbeidsgiver().getIdentifikator().equals(ORGNR3)).findFirst().get();

        // Forventer at ORGNR1 har fått økt sitt brutto bg
        BigDecimal forventetNyBruttoForArbeid1 = BigDecimal.valueOf(240_000);
        assertThat(andel1.getFordeltPrÅr()).isEqualByComparingTo(forventetNyBruttoForArbeid1);
        // Forventer at brutto for arbeid for ORGNR2 er uendret ettersom den ikkje har disponibelt grunnlag å fordele (søker full refusjon)
        assertThat(andel2.getFordeltPrÅr()).isNull();
        // Forventer at ORGNR2 har fått redusert sitt brutto bg
        BigDecimal forventetNyBruttoForArbeid3 = BigDecimal.valueOf(120_000);
        assertThat(andel3.getFordeltPrÅr()).isEqualByComparingTo(forventetNyBruttoForArbeid3);
    }

    private FastsettBeregningsgrunnlagPerioderTjeneste lagTjeneste() {
        InntektsmeldingMedRefusjonTjeneste finnFørste = new InntektsmeldingMedRefusjonTjeneste(inntektsmeldingTjeneste);
        var oversetterTilRegelNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse(finnFørste);
        var oversetterTilRegelRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering(finnFørste);
        var oversetterFraRegelTilVLNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse();
        var oversetterFraRegelTilVLRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering();
        return new FastsettBeregningsgrunnlagPerioderTjeneste(oversetterTilRegelNaturalytelse,
            new UnitTestLookupInstanceImpl<>(oversetterTilRegelRefusjonOgGradering), oversetterFraRegelTilVLNaturalytelse,
            oversetterFraRegelTilVLRefusjonOgGradering);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag(Map<String, BigDecimal> orgnrs, BehandlingReferanse behandlingReferanse,
                                                                    BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = lagBeregningsgrunnlagPerioderBuilder(SKJÆRINGSTIDSPUNKT, null, orgnrs);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medHjemmel(Hjemmel.F_14_7));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagBuilder.build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg)
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.FORESLÅTT);
    }

    private BeregningsgrunnlagPeriode.Builder lagBeregningsgrunnlagPerioderBuilder(LocalDate fom, LocalDate tom, Map<String, BigDecimal> orgnrs) {
        BeregningsgrunnlagPeriode.Builder builder = BeregningsgrunnlagPeriode.builder();
        for (String orgnr : orgnrs.keySet()) {
            Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
            BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBeregnetPrÅr(orgnrs.get(orgnr))
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(arbeidsgiver)
                    .medArbeidsperiodeFom(SKJÆRINGSTIDSPUNKT.minusYears(1)));
            builder.leggTilBeregningsgrunnlagPrStatusOgAndel(andelBuilder);
        }
        return builder
            .medBeregningsgrunnlagPeriode(fom, tom);
    }

    private void leggTilYrkesaktiviteterOgBeregningAktiviteter(AbstractTestScenario<?> scenario, List<String> orgnrs) {
        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        for (String orgnr : orgnrs) {
            Arbeidsgiver arbeidsgiver = leggTilYrkesaktivitet(arbeidsperiode1, aktørArbeidBuilder, orgnr);
            fjernOgLeggTilNyBeregningAktivitet(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato(), arbeidsgiver, InternArbeidsforholdRef.nullRef());
        }

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private void fjernOgLeggTilNyBeregningAktivitet(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        if (fom.isAfter(SKJÆRINGSTIDSPUNKT)) {
            throw new IllegalArgumentException("Kan ikke lage BeregningAktivitet som starter etter skjæringstidspunkt");
        }
        aktiviteter.add(lagAktivitet(fom, tom, arbeidsgiver, arbeidsforholdRef));
    }


    private BeregningAktivitetEntitet lagAktivitet(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();
    }

    private Arbeidsgiver leggTilYrkesaktivitet(DatoIntervallEntitet arbeidsperiode, InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                               String orgnr) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        AktivitetsAvtaleBuilder aaBuilder1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);
        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
        return arbeidsgiver;
    }


}
