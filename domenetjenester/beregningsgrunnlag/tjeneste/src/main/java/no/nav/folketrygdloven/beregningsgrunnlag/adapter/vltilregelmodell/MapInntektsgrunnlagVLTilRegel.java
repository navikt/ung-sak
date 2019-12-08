package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningUtils;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.NaturalYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Periodeinntekt;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class MapInntektsgrunnlagVLTilRegel {

    private static final String TOGGLE_SPLITTE_SAMMENLIGNING = "fpsak.splitteSammenligningATFL";
    private int inntektRapporteringFristDag;
    private Unleash unleash;

    protected MapInntektsgrunnlagVLTilRegel() {
        // CDI
    }

    /**
     * @param inntektRapporteringFristDagIMåneden - dag i måneden inntekt rapporteres
     */
    @Inject
    public MapInntektsgrunnlagVLTilRegel(@KonfigVerdi(value = "inntekt.rapportering.frist.dato", defaultVerdi = "5") int inntektRapporteringFristDagIMåneden,
                                         Unleash unleash) {
        this.inntektRapporteringFristDag = inntektRapporteringFristDagIMåneden;
        this.unleash = unleash;

    }

    private static void lagInntektBeregning(Inntektsgrunnlag inntektsgrunnlag, InntektFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        filter.filterBeregningsgrunnlag()
            .filter(i -> i.getArbeidsgiver() != null)
            .forFilter((inntekt, inntektsposter) -> mapInntekt(inntektsgrunnlag, inntekt, inntektsposter, yrkesaktiviteter));
    }

    private static void mapInntekt(Inntektsgrunnlag inntektsgrunnlag, Inntekt inntekt, Collection<Inntektspost> inntektsposter,
                                   Collection<Yrkesaktivitet> yrkesaktiviteter) {
        inntektsposter.forEach(inntektspost -> {

            Arbeidsforhold arbeidsgiver = mapYrkesaktivitet(inntekt.getArbeidsgiver(), yrkesaktiviteter);
            if (Objects.isNull(arbeidsgiver)) {
                throw new IllegalStateException("Arbeidsgiver må være satt.");
            } else if (Objects.isNull(inntektspost.getPeriode().getFomDato())) {
                throw new IllegalStateException("Inntektsperiode må være satt.");
            } else if (Objects.isNull(inntektspost.getBeløp().getVerdi())) {
                throw new IllegalStateException("Inntektsbeløp må være satt.");
            }

            inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
                .medInntektskildeOgPeriodeType(Inntektskilde.INNTEKTSKOMPONENTEN_BEREGNING)
                .medArbeidsgiver(arbeidsgiver)
                .medMåned(inntektspost.getPeriode().getFomDato())
                .medInntekt(inntektspost.getBeløp().getVerdi())
                .build());
        });
    }

    private static Arbeidsforhold mapYrkesaktivitet(Arbeidsgiver arbeidsgiver, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        return erFrilanser(arbeidsgiver, yrkesaktiviteter)
            ? Arbeidsforhold.frilansArbeidsforhold()
            : lagNyttArbeidsforholdHosArbeidsgiver(arbeidsgiver);
    }

    private static boolean erFrilanser(Arbeidsgiver arbeidsgiver, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        final List<ArbeidType> arbeidType = yrkesaktiviteter
            .stream()
            // TODO(OJR) hva gjør vi med yrkesaktiviter som ikke har arbeidsgiver? Sånns som militær-tjeneste??
            .filter(it -> it.getArbeidsgiver() != null)
            .filter(it -> it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()))
            .map(Yrkesaktivitet::getArbeidType)
            .distinct()
            .collect(Collectors.toList());
        boolean erFrilanser = yrkesaktiviteter.stream()
            .map(Yrkesaktivitet::getArbeidType)
            .anyMatch(ArbeidType.FRILANSER::equals);
        return (arbeidType.isEmpty() && erFrilanser) || arbeidType.contains(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
    }

    private static Arbeidsforhold lagNyttArbeidsforholdHosArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getErVirksomhet()) {
            return Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbeidsgiver.getIdentifikator());
        } else if (arbeidsgiver.erAktørId()) {
            return Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbeidsgiver.getIdentifikator());
        }
        throw new IllegalStateException("Arbeidsgiver må være enten aktør eller virksomhet, men var: " + arbeidsgiver);
    }

    private static void mapInntektsmelding(Inntektsgrunnlag inntektsgrunnlag, Collection<Inntektsmelding>inntektsmeldinger) {
        inntektsmeldinger.forEach(im -> {
            Arbeidsforhold arbeidsforhold = MapArbeidsforholdFraVLTilRegel.mapForInntektsmelding(im);
            BigDecimal inntekt = im.getInntektBeløp().getVerdi();
            List<NaturalYtelse> naturalytelser = im.getNaturalYtelser().stream()
                .map(ny -> new NaturalYtelse(ny.getBeloepPerMnd().getVerdi(), ny.getPeriode().getFomDato(), ny.getPeriode().getTomDato()))
                .collect(Collectors.toList());
            Periodeinntekt.Builder naturalYtelserBuilder = Periodeinntekt.builder()
                .medInntektskildeOgPeriodeType(Inntektskilde.INNTEKTSMELDING)
                .medArbeidsgiver(arbeidsforhold)
                .medInntekt(inntekt)
                .medNaturalYtelser(naturalytelser);
            im.getStartDatoPermisjon().ifPresent(dato -> naturalYtelserBuilder.medMåned(dato.minusMonths(1).withDayOfMonth(1)));

            inntektsgrunnlag.leggTilPeriodeinntekt(naturalYtelserBuilder.build());
        });
    }

    private static void mapTilstøtendeYtelserDagpengerOgAAP(Inntektsgrunnlag inntektsgrunnlag,
                                                            YtelseFilter ytelseFilter,
                                                            LocalDate skjæringstidspunkt) {

        Optional<Ytelse> nyesteVedtakForDagsats = BeregningUtils.sisteVedtakFørStpForType(ytelseFilter, skjæringstidspunkt,
            Set.of(RelatertYtelseType.DAGPENGER, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER));
        if (nyesteVedtakForDagsats.isEmpty()) {
            return;
        }

        Optional<YtelseAnvist> sisteUtbetalingFørStp = BeregningUtils.sisteHeleMeldekortFørStp(ytelseFilter, nyesteVedtakForDagsats.get(), skjæringstidspunkt,
            Set.of(RelatertYtelseType.DAGPENGER, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER));
        BigDecimal dagsats = nyesteVedtakForDagsats.get().getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats).map(Beløp::getVerdi)
            .orElse(sisteUtbetalingFørStp.flatMap(YtelseAnvist::getDagsats).map(Beløp::getVerdi).orElse(BigDecimal.ZERO));
        BigDecimal utbetalingsgradProsent = sisteUtbetalingFørStp.flatMap(YtelseAnvist::getUtbetalingsgradProsent)
            .map(Stillingsprosent::getVerdi).orElse(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG);

        inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.TILSTØTENDE_YTELSE_DP_AAP)
            .medInntekt(dagsats)
            .medMåned(skjæringstidspunkt)
            .medUtbetalingsgrad(utbetalingsgradProsent)
            .build());
    }

    private static void lagInntektSammenligning(Inntektsgrunnlag inntektsgrunnlag, InntektFilter filter) {
        Map<LocalDate, BigDecimal> månedsinntekter = filter.filterSammenligningsgrunnlag().getFiltrertInntektsposter().stream()
            .collect(Collectors.groupingBy(ip -> ip.getPeriode().getFomDato(), Collectors.reducing(BigDecimal.ZERO,
                ip -> ip.getBeløp().getVerdi(), BigDecimal::add)));

        månedsinntekter.forEach((måned, inntekt) -> inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING)
            .medMåned(måned)
            .medInntekt(inntekt)
            .build()));
    }

    private static void lagInntektSammenligningPrStatus(Inntektsgrunnlag inntektsgrunnlag, InntektFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        Map<LocalDate, BigDecimal> månedsinntekterFrilans = filter.filterSammenligningsgrunnlag().getAlleInntektSammenligningsgrunnlag().stream()
            .filter(inntekt -> erFrilanser(inntekt.getArbeidsgiver(), yrkesaktiviteter))
            .flatMap(i -> i.getAlleInntektsposter().stream())
            .collect(Collectors.groupingBy(ip -> ip.getPeriode().getFomDato(), Collectors.reducing(BigDecimal.ZERO,
                ip -> ip.getBeløp().getVerdi(), BigDecimal::add)));

        månedsinntekterFrilans.forEach((måned, inntekt) -> inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING)
            .medMåned(måned)
            .medInntekt(inntekt)
            .medAktivitetStatus(AktivitetStatus.FL)
            .build()));

        Map<LocalDate, BigDecimal> månedsinntekterArbeidstaker = filter.filterSammenligningsgrunnlag().getAlleInntektSammenligningsgrunnlag().stream()
            .filter(inntekt -> !erFrilanser(inntekt.getArbeidsgiver(), yrkesaktiviteter))
            .flatMap(i -> i.getAlleInntektsposter().stream())
            .collect(Collectors.groupingBy(ip -> ip.getPeriode().getFomDato(), Collectors.reducing(BigDecimal.ZERO,
                ip -> ip.getBeløp().getVerdi(), BigDecimal::add)));

        månedsinntekterArbeidstaker.forEach((måned, inntekt) -> inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING)
            .medMåned(måned)
            .medInntekt(inntekt)
            .medAktivitetStatus(AktivitetStatus.AT)
            .build()));
    }

    private static void lagInntekterSN(Inntektsgrunnlag inntektsgrunnlag, InntektFilter filter) {
        filter.filterBeregnetSkatt().getFiltrertInntektsposter()
            .forEach(inntektspost -> inntektsgrunnlag.leggTilPeriodeinntekt(Periodeinntekt.builder()
                .medInntektskildeOgPeriodeType(Inntektskilde.SIGRUN)
                .medInntekt(inntektspost.getBeløp().getVerdi())
                .medPeriode(Periode.of(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato()))
                .build()));
    }

    private static void mapOppgittOpptjening(Inntektsgrunnlag inntektsgrunnlag, OppgittOpptjening oppgittOpptjening) {
        oppgittOpptjening.getEgenNæring().stream()
            .filter(en -> en.getNyoppstartet() || en.getVarigEndring())
            .filter(en -> en.getBruttoInntekt() != null)
            .forEach(en -> inntektsgrunnlag.leggTilPeriodeinntekt(byggPeriodeinntektEgenNæring(en)));
    }

    private static Periodeinntekt byggPeriodeinntektEgenNæring(OppgittEgenNæring en) {
        LocalDate datoForInntekt;
        if (en.getVarigEndring()) {
            datoForInntekt = en.getEndringDato();
        } else {
            datoForInntekt = en.getFraOgMed();
        }
        if (datoForInntekt == null) {
            throw new IllegalStateException("Søker har oppgitt varig endret eller nyoppstartet næring men har ikke oppgitt endringsdato eller oppstartsdato");
        }
        return Periodeinntekt.builder()
            .medInntektskildeOgPeriodeType(Inntektskilde.SØKNAD)
            .medMåned(datoForInntekt)
            .medInntekt(en.getBruttoInntekt())
            .build();
    }

    public Inntektsgrunnlag map(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, LocalDate skjæringstidspunktBeregning, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Inntektsgrunnlag inntektsgrunnlag = new Inntektsgrunnlag();
        inntektsgrunnlag.setInntektRapporteringFristDag(inntektRapporteringFristDag);
        hentInntektArbeidYtelse(referanse, inntektsgrunnlag, inntektsmeldinger, skjæringstidspunktBeregning, iayGrunnlag);

        return inntektsgrunnlag;
    }

    private void hentInntektArbeidYtelse(BehandlingReferanse referanse, Inntektsgrunnlag inntektsgrunnlag, Collection<Inntektsmelding> inntektsmeldinger, LocalDate skjæringstidspunktBeregning, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        AktørId aktørId = referanse.getAktørId();

        var filter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunktBeregning);

        if (!filter.isEmpty()) {
            var aktørArbeid = iayGrunnlag.getAktørArbeidFraRegister(aktørId);
            var filterYaRegister = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), aktørArbeid).før(skjæringstidspunktBeregning);
            List<Yrkesaktivitet> yrkesaktiviteter = new ArrayList<>();
            yrkesaktiviteter.addAll(filterYaRegister.getYrkesaktiviteterForBeregning());
            yrkesaktiviteter.addAll(filterYaRegister.getFrilansOppdrag());

            // TODO (OJR): trenger denne filtrere på arbeidsforholdInformasjon?
            var bekreftetAnnenOpptjening = iayGrunnlag.getBekreftetAnnenOpptjening(aktørId);
            var filterYaBekreftetAnnenOpptjening = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), bekreftetAnnenOpptjening).før(skjæringstidspunktBeregning);
            yrkesaktiviteter.addAll(filterYaBekreftetAnnenOpptjening.getYrkesaktiviteterForBeregning());

            lagInntektBeregning(inntektsgrunnlag, filter, yrkesaktiviteter);
            if (!unleash.isEnabled(TOGGLE_SPLITTE_SAMMENLIGNING, false)) {
                lagInntektSammenligning(inntektsgrunnlag, filter);
            } else {
                lagInntektSammenligningPrStatus(inntektsgrunnlag, filter, yrkesaktiviteter);
            }
            lagInntekterSN(inntektsgrunnlag, filter);
        }

        mapInntektsmelding(inntektsgrunnlag, inntektsmeldinger);

        var ytelseFilter = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunktBeregning);
        if (!ytelseFilter.getFiltrertYtelser().isEmpty()) {
            mapTilstøtendeYtelserDagpengerOgAAP(inntektsgrunnlag, ytelseFilter, skjæringstidspunktBeregning);
        }

        Optional<OppgittOpptjening> oppgittOpptjeningOpt = iayGrunnlag.getOppgittOpptjening();
        oppgittOpptjeningOpt.ifPresent(oppgittOpptjening -> mapOppgittOpptjening(inntektsgrunnlag, oppgittOpptjening));

    }
}
