package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;

public class VerdikjedeTestHjelper {

    static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);

    private final AtomicLong journalpostIdInc = new AtomicLong(123);
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    public VerdikjedeTestHjelper() {
    }

    public VerdikjedeTestHjelper(InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    void verifiserPeriode(BeregningsgrunnlagPeriode periode, LocalDate fom, LocalDate tom, int antallAndeler) {
        verifiserPeriode(periode, fom, tom, antallAndeler, null);
    }

    void verifiserPeriode(BeregningsgrunnlagPeriode periode, LocalDate fom, LocalDate tom, int antallAndeler, Long dagsats) {
        assertThat(periode.getBeregningsgrunnlagPeriodeFom()).isEqualTo(fom);
        assertThat(periode.getBeregningsgrunnlagPeriodeTom()).isEqualTo(tom);
        assertThat(periode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(antallAndeler);
        assertThat(periode.getDagsats()).isEqualTo(dagsats);
    }

    void verifiserBeregningsgrunnlagBasis(BeregningsgrunnlagEntitet beregningsgrunnlag, Hjemmel hjemmel) {
        assertThat(beregningsgrunnlag).isNotNull();
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(beregningsgrunnlag.getHjemmel()).isEqualTo(hjemmel);
    }

    void verifiserBeregningsgrunnlagBasis(BeregningsgrunnlagRegelResultat beregningsgrunnlagRegelResultat, Hjemmel hjemmel) {
        assertThat(beregningsgrunnlagRegelResultat.getAksjonspunkter()).isEmpty();
        verifiserBeregningsgrunnlagBasis(beregningsgrunnlagRegelResultat.getBeregningsgrunnlag(), hjemmel);

    }

    void verifiserSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag, double rapportertPrÅr, LocalDate fom, LocalDate tom,
                                         Long avvikPromille) {
        assertThat(sammenligningsgrunnlag.getRapportertPrÅr().doubleValue()).isEqualTo(rapportertPrÅr, within(0.01));
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeFom()).isEqualTo(fom);
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeTom()).isEqualTo(tom);
        assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(avvikPromille);
    }

    void verifiserBGATførAvkorting(BeregningsgrunnlagPeriode periode, List<Double> bgListe, List<String> virksomheterOrgnr) {
        List<BeregningsgrunnlagPrStatusOgAndel> bgpsaListe = statusliste(periode, AktivitetStatus.ARBEIDSTAKER);
        for (int ix = 0; ix < bgpsaListe.size(); ix++) {
            BeregningsgrunnlagPrStatusOgAndel bgpsa = bgpsaListe.get(ix);
            assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
            final int index = ix;
            assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
                .hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver.getOrgnr()).isEqualTo(virksomheterOrgnr.get(index)));
            assertThat(bgpsa.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
                .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
                    .as("gjelderSpesifiktArbeidsforhold").isFalse();
            assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
            assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(bgListe.get(ix), within(0.01));
            assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(bgListe.get(ix), within(0.01));

            assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
            assertThat(bgpsa.getAvkortetPrÅr()).isNull();
            assertThat(bgpsa.getRedusertPrÅr()).isNull();

            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

            assertThat(bgpsa.getAvkortetBrukersAndelPrÅr()).isNull();
            assertThat(bgpsa.getRedusertBrukersAndelPrÅr()).isNull();

            assertThat(bgpsa.getMaksimalRefusjonPrÅr()).isNull();
            assertThat(bgpsa.getAvkortetRefusjonPrÅr()).isNull();
            assertThat(bgpsa.getRedusertRefusjonPrÅr()).isNull();
        }
    }

    void verifiserBGATetterAvkorting(BeregningsgrunnlagPeriode periode,
                                     List<Double> beregnetListe,
                                     List<Double> bruttoBgListe, List<String> virksomheteneOrgnr,
                                     List<Double> avkortetListe,
                                     List<Double> maksimalRefusjonListe,
                                     List<Double> avkortetRefusjonListe,
                                     List<Double> avkortetBrukersAndelListe, boolean overstyrt) {
        List<BeregningsgrunnlagPrStatusOgAndel> bgpsaListe = statusliste(periode, AktivitetStatus.ARBEIDSTAKER);
        assertThat(beregnetListe).hasSameSizeAs(bgpsaListe);
        assertThat(avkortetListe).hasSameSizeAs(bgpsaListe);
        assertThat(maksimalRefusjonListe).hasSameSizeAs(bgpsaListe);
        assertThat(avkortetRefusjonListe).hasSameSizeAs(bgpsaListe);
        assertThat(avkortetBrukersAndelListe).hasSameSizeAs(bgpsaListe);
        for (int ix = 0; ix < bgpsaListe.size(); ix++) {
            BeregningsgrunnlagPrStatusOgAndel bgpsa = bgpsaListe.get(ix);
            assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
            final int index = ix;
            assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
                .hasValueSatisfying(arbeidsgiver -> assertThat(arbeidsgiver.getOrgnr()).isEqualTo(virksomheteneOrgnr.get(index)));
            assertThat(bgpsa.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
                .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
                    .as("gjelderSpesifiktArbeidsforhold").isFalse();
            assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
            assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(beregnetListe.get(ix), within(0.01));
            assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(bruttoBgListe.get(ix), within(0.01));

            if (!overstyrt) {
                assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
            }
            assertThat(bgpsa.getAvkortetPrÅr().doubleValue()).isCloseTo(avkortetListe.get(ix), within(0.01));
            assertThat(bgpsa.getRedusertPrÅr().doubleValue()).isCloseTo(avkortetListe.get(ix), within(0.01));

            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

            assertThat(bgpsa.getMaksimalRefusjonPrÅr().doubleValue()).as("MaksimalRefusjonPrÅr")
                .isCloseTo(maksimalRefusjonListe.get(ix), within(0.01));
            assertThat(bgpsa.getAvkortetRefusjonPrÅr().doubleValue()).as("AvkortetRefusjonPrÅr")
                .isCloseTo(avkortetRefusjonListe.get(ix), within(0.01));
            assertThat(bgpsa.getRedusertRefusjonPrÅr().doubleValue()).as("RedusertRefusjonPrÅr")
                .isCloseTo(avkortetRefusjonListe.get(ix), within(0.01));

            assertThat(bgpsa.getAvkortetBrukersAndelPrÅr().doubleValue()).as("AvkortetBrukersAndelPrÅr")
                .isCloseTo(avkortetBrukersAndelListe.get(ix), within(0.01));
            assertThat(bgpsa.getRedusertBrukersAndelPrÅr().doubleValue()).as("RedusertBrukersAndelPrÅr")
                .isCloseTo(avkortetBrukersAndelListe.get(ix), within(0.01));
        }
    }

    void verifiserFLførAvkorting(BeregningsgrunnlagPeriode periode, Double bgFL) {
        List<BeregningsgrunnlagPrStatusOgAndel> bgpsaListe = statusliste(periode, AktivitetStatus.FRILANSER);
        assertThat(bgpsaListe).hasSize(1);
        for (BeregningsgrunnlagPrStatusOgAndel bgpsa : bgpsaListe) {
            assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
            assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(bgFL);
            assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(bgFL);

            assertThat(bgpsa.getBeregningsperiodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3).withDayOfMonth(1));
            assertThat(bgpsa.getBeregningsperiodeTom()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1));

            assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
            assertThat(bgpsa.getAvkortetPrÅr()).isNull();
            assertThat(bgpsa.getRedusertPrÅr()).isNull();

            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

            assertThat(bgpsa.getAvkortetBrukersAndelPrÅr()).isNull();
            assertThat(bgpsa.getRedusertBrukersAndelPrÅr()).isNull();

            assertThat(bgpsa.getMaksimalRefusjonPrÅr()).isNull();
            assertThat(bgpsa.getAvkortetRefusjonPrÅr()).isNull();
            assertThat(bgpsa.getRedusertRefusjonPrÅr()).isNull();
        }
    }

    void verifiserFLetterAvkorting(BeregningsgrunnlagPeriode periode, Double beregnetFL, Double bgFL, Double avkortetBgFL, Double brukersAndelFL) {
        List<BeregningsgrunnlagPrStatusOgAndel> bgpsaListe = statusliste(periode, AktivitetStatus.FRILANSER);
        assertThat(bgpsaListe).hasSize(1);
        for (BeregningsgrunnlagPrStatusOgAndel bgpsa : bgpsaListe) {
            assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
            assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(beregnetFL);
            assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(bgFL);

            assertThat(bgpsa.getBeregningsperiodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3).withDayOfMonth(1));
            assertThat(bgpsa.getBeregningsperiodeTom()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1));

            assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
            assertThat(bgpsa.getAvkortetPrÅr().doubleValue()).isEqualTo(avkortetBgFL, within(0.01));
            assertThat(bgpsa.getRedusertPrÅr().doubleValue()).isEqualTo(avkortetBgFL, within(0.01));

            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

            assertThat(bgpsa.getAvkortetBrukersAndelPrÅr().doubleValue()).isCloseTo(brukersAndelFL, within(0.01));
            assertThat(bgpsa.getRedusertBrukersAndelPrÅr().doubleValue()).isCloseTo(brukersAndelFL, within(0.01));

            assertThat(bgpsa.getMaksimalRefusjonPrÅr().doubleValue()).isEqualTo(0.0d);
            assertThat(bgpsa.getAvkortetRefusjonPrÅr().doubleValue()).isEqualTo(0.0d);
            assertThat(bgpsa.getRedusertRefusjonPrÅr().doubleValue()).isEqualTo(0.0d);
        }
    }

    void verifiserBGSNførAvkorting(BeregningsgrunnlagPeriode periode, double forventetBrutto, double forventetBeregnet, int sisteÅr) {
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = statusliste(periode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andeler).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = andeler.get(0);
        assertThat(andel.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)).isEmpty();
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef)).isEmpty();
        assertThat(andel.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.NÆRING);
        assertThat(andel.getBeregnetPrÅr().doubleValue()).isEqualTo(forventetBeregnet, within(0.2));
        assertThat(andel.getBruttoPrÅr().doubleValue()).isEqualTo(forventetBrutto, within(0.2));

        assertThat(andel.getBeregningsperiodeFom()).isEqualTo(LocalDate.of(sisteÅr - 2, Month.JANUARY, 1));
        assertThat(andel.getBeregningsperiodeTom()).isEqualTo(LocalDate.of(sisteÅr, Month.DECEMBER, 31));

        assertThat(andel.getOverstyrtPrÅr()).isNull();
        assertThat(andel.getAvkortetPrÅr()).isNull();
        assertThat(andel.getRedusertPrÅr()).isNull();

        assertThat(andel.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

        assertThat(andel.getAvkortetBrukersAndelPrÅr()).isNull();
        assertThat(andel.getRedusertBrukersAndelPrÅr()).isNull();

        assertThat(andel.getMaksimalRefusjonPrÅr()).isNull();
        assertThat(andel.getAvkortetRefusjonPrÅr()).isNull();
        assertThat(andel.getRedusertRefusjonPrÅr()).isNull();

        assertThat(andel.getPgiSnitt()).isNotNull();
        assertThat(andel.getPgi1()).isNotNull();
        assertThat(andel.getPgi2()).isNotNull();
        assertThat(andel.getPgi3()).isNotNull();
    }

    void verifiserBGSNetterAvkorting(BeregningsgrunnlagPeriode periode, double forventetBeregnet, double forventetBrutto,
                                     double forventetAvkortet, double forventetRedusert, int sisteÅr) {
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = statusliste(periode, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andeler).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndel andel = andeler.get(0);
        assertThat(andel.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)).isEmpty();
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef)).isEmpty();
        assertThat(andel.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.NÆRING);
        assertThat(andel.getBeregnetPrÅr().doubleValue()).isEqualTo(forventetBeregnet, within(0.2));
        assertThat(andel.getBruttoPrÅr().doubleValue()).isEqualTo(forventetBrutto, within(0.2));

        assertThat(andel.getBeregningsperiodeFom()).isEqualTo(LocalDate.of(sisteÅr - 2, Month.JANUARY, 1));
        assertThat(andel.getBeregningsperiodeTom()).isEqualTo(LocalDate.of(sisteÅr, Month.DECEMBER, 31));

        assertThat(andel.getOverstyrtPrÅr()).isNull();
        assertThat(andel.getAvkortetPrÅr().doubleValue()).isEqualTo(forventetAvkortet, within(0.2));
        assertThat(andel.getRedusertPrÅr().doubleValue()).isEqualTo(forventetRedusert, within(0.2));

        assertThat(andel.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).isEmpty();

        assertThat(andel.getMaksimalRefusjonPrÅr().doubleValue()).isEqualTo(0.0);
        assertThat(andel.getAvkortetRefusjonPrÅr().doubleValue()).isEqualTo(0.0);
        assertThat(andel.getRedusertRefusjonPrÅr().doubleValue()).isEqualTo(0.0);

        assertThat(andel.getAvkortetBrukersAndelPrÅr().doubleValue()).isEqualTo(forventetAvkortet, within(0.2));
        assertThat(andel.getRedusertBrukersAndelPrÅr().doubleValue()).isEqualTo(forventetRedusert, within(0.2));

        assertThat(andel.getPgiSnitt()).isNotNull();
        assertThat(andel.getPgi1()).isNotNull();
        assertThat(andel.getPgi2()).isNotNull();
        assertThat(andel.getPgi3()).isNotNull();
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> statusliste(BeregningsgrunnlagPeriode periode, AktivitetStatus status) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bpsa -> status.equals(bpsa.getAktivitetStatus()))
            .sorted(Comparator.comparing(bga -> bga.getBgAndelArbeidsforhold().get().getArbeidsforholdOrgnr()))
            .collect(Collectors.toList());
    }

    private Inntektsmelding lagreInntektsmelding(BigDecimal beløp, BehandlingReferanse behandlingReferanse,
                                                 Arbeidsgiver arbeidsgiver,
                                                 BigDecimal refusjonskrav, NaturalYtelse naturalYtelse) {

        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        inntektsmeldingBuilder.medStartDatoPermisjon(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        inntektsmeldingBuilder.medInnsendingstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.atStartOfDay());
        inntektsmeldingBuilder.medBeløp(beløp);
        inntektsmeldingBuilder.medJournalpostId(new JournalpostId(journalpostIdInc.getAndIncrement()));
        if (naturalYtelse != null) {
            inntektsmeldingBuilder.leggTil(naturalYtelse);
        }
        if (refusjonskrav != null) {
            inntektsmeldingBuilder.medRefusjon(refusjonskrav);
        }

        inntektsmeldingBuilder.medArbeidsgiver(arbeidsgiver);

        if (inntektsmeldingTjeneste != null) {
            inntektsmeldingTjeneste.lagreInntektsmelding(behandlingReferanse.getSaksnummer(), behandlingReferanse.getId(), inntektsmeldingBuilder);
        }

        return inntektsmeldingBuilder.build();
    }

    public void initBehandlingFor_AT_SN(AbstractTestScenario<?> scenario,
                                        BigDecimal skattbarInntekt,
                                        int førsteÅr, LocalDate skjæringstidspunkt, String virksomhetOrgnr,
                                        BigDecimal inntektSammenligningsgrunnlag,
                                        BigDecimal inntektBeregningsgrunnlag) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        for (LocalDate året = LocalDate.of(førsteÅr, Month.JANUARY, 1); året.getYear() < førsteÅr + 3; året = året.plusYears(1)) {
            lagInntektForSN(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), året, skattbarInntekt);
        }
        LocalDate fraOgMed = skjæringstidspunkt.minusYears(1).withDayOfMonth(1);
        LocalDate tilOgMed = fraOgMed.plusYears(1);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), arbeidsgiver, fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            lagInntektForSammenligning(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                arbeidsgiver);
            lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektBeregningsgrunnlag,
                arbeidsgiver);
            lagInntektForOpptjening(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektBeregningsgrunnlag,
                virksomhetOrgnr);
        }
    }

    public void lagBehandlingForSN(AbstractTestScenario<?> scenario,
                                   BigDecimal skattbarInntekt,
                                   int førsteÅr) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        for (LocalDate året = LocalDate.of(førsteÅr, Month.JANUARY, 1); året.getYear() < førsteÅr + 3; året = året.plusYears(1)) {
            lagInntektForSN(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), året, skattbarInntekt);
        }
    }

    private void lagInntektForSN(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId,
                                 LocalDate år, BigDecimal årsinntekt) {
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);
        InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.SIGRUN, null);
        InntektspostBuilder inntektspost = InntektspostBuilder.ny()
            .medBeløp(årsinntekt)
            .medPeriode(år.withMonth(1).withDayOfMonth(1), år.withMonth(12).withDayOfMonth(31))
            .medInntektspostType(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE);
        inntektBuilder.leggTilInntektspost(inntektspost);
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
    }

    public void initBehandlingFL(AbstractTestScenario<?> scenario, BigDecimal inntektSammenligningsgrunnlag,
                                 BigDecimal inntektFrilans,
                                 String virksomhetOrgnr, LocalDate fraOgMed, LocalDate tilOgMed) {

        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), arbeidsgiver, fraOgMed, tilOgMed, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektFrilans,
                arbeidsgiver);
            lagInntektForSammenligning(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                arbeidsgiver);
            lagInntektForOpptjening(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                virksomhetOrgnr);
        }

    }

    public YrkesaktivitetBuilder lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId,
                                                Arbeidsgiver arbeidsgiver,
                                                LocalDate fom, LocalDate tom, ArbeidType arbeidType) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsgiver(arbeidsgiver);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder
            .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);

        return yrkesaktivitetBuilder;
    }

    public void lagInntektForSammenligning(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
                                           AktørId aktørId, LocalDate fom,
                                           LocalDate tom, BigDecimal månedsbeløp, Arbeidsgiver arbeidsgiver) {
        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsgiver(arbeidsgiver);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        InntektsKilde kilde = InntektsKilde.INNTEKT_SAMMENLIGNING;
        InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
        InntektspostBuilder inntektspost = InntektspostBuilder.ny()
            .medBeløp(månedsbeløp)
            .medPeriode(fom, tom)
            .medInntektspostType(InntektspostType.LØNN);
        inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(arbeidsgiver);
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
    }

    public void lagInntektForArbeidsforhold(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
                                            AktørId aktørId, LocalDate fom,
                                            LocalDate tom, BigDecimal månedsbeløp, Arbeidsgiver arbeidsgiver) {
        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forArbeidsgiver(arbeidsgiver);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        InntektsKilde kilde = InntektsKilde.INNTEKT_BEREGNING;
        InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
        InntektspostBuilder inntektspost = InntektspostBuilder.ny()
            .medBeløp(månedsbeløp)
            .medPeriode(fom, tom)
            .medInntektspostType(InntektspostType.LØNN);
        inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(arbeidsgiver);
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
    }

    void lagInntektForOpptjening(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
                                 AktørId aktørId, LocalDate fom,
                                 LocalDate tom, BigDecimal månedsbeløp, String virksomhetOrgnr) {
        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        InntektsKilde kilde = InntektsKilde.INNTEKT_OPPTJENING;
        InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
        InntektspostBuilder inntektspost = InntektspostBuilder.ny()
            .medBeløp(månedsbeløp)
            .medPeriode(fom, tom)
            .medInntektspostType(InntektspostType.LØNN);
        inntektBuilder.leggTilInntektspost(inntektspost)
            .medArbeidsgiver(aktørId == null ? Arbeidsgiver.virksomhet(virksomhetOrgnr) : Arbeidsgiver.person(aktørId));
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
    }

    Inntektsmelding opprettInntektsmeldingMedRefusjonskrav(BehandlingReferanse behandlingReferanse, Arbeidsgiver arbeidsgiver, BigDecimal inntektInntektsmelding,
                                                           BigDecimal refusjonskrav) {
        return opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, arbeidsgiver, inntektInntektsmelding, null, refusjonskrav);
    }

    public Inntektsmelding opprettInntektsmeldingMedRefusjonskrav(BehandlingReferanse behandlingReferanse, Arbeidsgiver arbeidsgiver, BigDecimal inntektInntektsmelding,
                                                                  NaturalYtelse naturalYtelse,
                                                                  BigDecimal refusjonskrav) {
        return lagreInntektsmelding(inntektInntektsmelding, behandlingReferanse,
            arbeidsgiver,
            refusjonskrav,
            naturalYtelse);
    }

    BeregningsgrunnlagGrunnlagEntitet kjørStegOgLagreGrunnlag(BeregningsgrunnlagInput input,
                                                              BeregningTjenesteWrapper beregningTjenesteWrapper) {
        var ref = input.getBehandlingReferanse();
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = beregningTjenesteWrapper.getFastsettBeregningAktiviteter().fastsettAktiviteter(input);

        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTjenesteWrapper.getFastsettSkjæringstidspunktOgStatuser().fastsett(ref,
            beregningAktivitetAggregat, input.getIayGrunnlag());
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag(ref, beregningAktivitetAggregat, beregningsgrunnlag,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        beregningsgrunnlag = beregningTjenesteWrapper.getFastsettBeregningsgrunnlagPerioderTjeneste().fastsettPerioderForNaturalytelse(newInput, beregningsgrunnlag);
        return lagGrunnlag(ref, beregningAktivitetAggregat, beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlag(BehandlingReferanse ref, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                          BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(ref.getBehandlingId(), tilstand);
    }

    void lagBehandlingATogFLogSN(AbstractTestScenario<?> scenario,
                                 List<BigDecimal> inntektBeregningsgrunnlag,
                                 List<String> beregningVirksomhetOrgnr,
                                 BigDecimal inntektFrilans,
                                 List<BigDecimal> årsinntekterSN,
                                 int førsteÅr,
                                 BigDecimal årsinntektVarigEndring) {
        LocalDate fraOgMed = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1);
        LocalDate tilOgMed = fraOgMed.plusYears(1);

        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();

        beregningVirksomhetOrgnr
            .forEach(orgnr -> lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(),
                Arbeidsgiver.virksomhet(orgnr), fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));

        String dummyVirksomhetOrgnr = "999";
        Arbeidsgiver dummyVirksomhet = Arbeidsgiver.virksomhet(dummyVirksomhetOrgnr);
        if (inntektFrilans != null) {
            lagAktørArbeid(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dummyVirksomhet, fraOgMed,
                tilOgMed, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        }

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            for (int i = 0; i < beregningVirksomhetOrgnr.size(); i++) {
                String virksomhetOrgnr_i = beregningVirksomhetOrgnr.get(i);
                lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder,
                    scenario.getSøkerAktørId(),
                    dt, dt.plusMonths(1), inntektBeregningsgrunnlag.get(i),
                    Arbeidsgiver.virksomhet(virksomhetOrgnr_i));
                lagInntektForOpptjening(inntektArbeidYtelseBuilder,
                    scenario.getSøkerAktørId(),
                    dt, dt.plusMonths(1), inntektBeregningsgrunnlag.get(i),
                    virksomhetOrgnr_i);
            }
            if (inntektFrilans != null) {
                lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektFrilans,
                    dummyVirksomhet);
                lagInntektForOpptjening(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), dt, dt.plusMonths(1), inntektFrilans,
                    dummyVirksomhetOrgnr);
            }
        }

        if (årsinntekterSN != null) {
            for (int ix = 0; ix < 3; ix++) {
                lagInntektForSN(inntektArbeidYtelseBuilder, scenario.getSøkerAktørId(), LocalDate.of(førsteÅr + ix, Month.JANUARY, 1),
                    årsinntekterSN.get(ix));
            }
        }
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        EgenNæringBuilder egenNæringBuilder = EgenNæringBuilder.ny()
            .medBruttoInntekt(årsinntektVarigEndring)
            .medVarigEndring(årsinntektVarigEndring != null)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING))
            .medEndringDato(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1));
        oppgittOpptjeningBuilder
            .leggTilEgneNæringer(List.of(egenNæringBuilder));
        if (inntektFrilans != null) {
            OppgittAnnenAktivitet frilanserAktivitet = new OppgittAnnenAktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed),
                ArbeidType.FRILANSER);
            oppgittOpptjeningBuilder.leggTilAnnenAktivitet(frilanserAktivitet);
        }
        scenario.medOppgittOpptjening(oppgittOpptjeningBuilder);
    }
}
