package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
public class FinnInntektsmeldingForBeregning {

    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private boolean togglePsbMigrering;


    public FinnInntektsmeldingForBeregning() {
    }

    @Inject
    public FinnInntektsmeldingForBeregning(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           @KonfigVerdi(value = "PSB_INFOTRYGD_MIGRERING", required = false, defaultVerdi = "false") boolean toggleMigrering) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.iayTjeneste = iayTjeneste;
        this.togglePsbMigrering = toggleMigrering;
    }

    Set<Inntektsmelding> finnInntektsmeldinger(BehandlingReferanse referanse, List<BeregnInput> beregnInput) {
        var inntektsmeldingerForSak = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        var imTjeneste = finnInntektsmeldingForBeregningTjeneste(referanse);
        Set<Inntektsmelding> overstyrteInntektsmeldinger = togglePsbMigrering ? finnOverstyrteInntektsmeldinger(beregnInput, inntektsmeldingerForSak, imTjeneste) : Set.of();
        var inntektsmeldinger = new HashSet<Inntektsmelding>();
        inntektsmeldinger.addAll(overstyrteInntektsmeldinger);
        inntektsmeldinger.addAll(inntektsmeldingerForSak);
        return inntektsmeldinger;
    }

    private static Set<Inntektsmelding> finnOverstyrteInntektsmeldinger(List<BeregnInput> beregnInput, Set<Inntektsmelding> inntektsmeldingerForSak, InntektsmeldingerRelevantForBeregning imTjeneste) {
        return beregnInput.stream().flatMap(i -> {
            var mottatteInntektsmeldingerForPeriode = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerForSak, i.getVilkårsperiode());
            return i.getInputOverstyringPeriode().stream().flatMap(overstyrtPeriode -> mapInntektsmeldingerForOverstyrtPeriode(mottatteInntektsmeldingerForPeriode, overstyrtPeriode));
        }).collect(Collectors.toSet());
    }

    private static Stream<Inntektsmelding> mapInntektsmeldingerForOverstyrtPeriode(List<Inntektsmelding> mottatteInntektsmeldingerForPeriode, InputOverstyringPeriode overstyrtPeriode) {
        LocalDate stp = overstyrtPeriode.getSkjæringstidspunkt();
        return overstyrtPeriode.getAktivitetOverstyringer().stream()
            .filter(a -> a.getAktivitetStatus().erArbeidstaker())
            .map(a -> mapAktivitetTilInntektsmelding(a, stp, mottatteInntektsmeldingerForPeriode));
    }

    static Inntektsmelding mapAktivitetTilInntektsmelding(InputAktivitetOverstyring a,
                                                          LocalDate stp,
                                                          Collection<Inntektsmelding> inntektsmeldingerForSak) {
        var inntektsmeldingerForAktivitet = finnInntektsmeldingerMottattForAktivitet(inntektsmeldingerForSak, a);
        var summertRefusjonTidslinje = lagSummertRefusjontidslinje(stp, inntektsmeldingerForAktivitet);

        return mapInntektsmelding(stp, a, summertRefusjonTidslinje, inntektsmeldingerForAktivitet);
    }

    public static Set<Inntektsmelding> finnInntektsmeldingerMottattForAktivitet(Collection<Inntektsmelding> inntektsmeldingerForSak, InputAktivitetOverstyring a) {
        return inntektsmeldingerForSak.stream().filter(im -> im.getArbeidsgiver().equals(a.getArbeidsgiver()))
            .collect(Collectors.toSet());
    }

    public static LocalDateTimeline<BigDecimal> lagSummertRefusjontidslinje(LocalDate stp, Collection<Inntektsmelding> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet.stream()
            .map(im -> tilRefusjontidslinje(im, stp))
            .reduce((tidslinje1, tidslinje2) -> tidslinje1.combine(tidslinje2, StandardCombinators::sum, LocalDateTimeline.JoinStyle.CROSS_JOIN))
            .orElse(LocalDateTimeline.empty())
            .compress();
    }


    private static Inntektsmelding mapInntektsmelding(LocalDate stp, InputAktivitetOverstyring a, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje, Collection<Inntektsmelding> inntektsmeldingerForAktivitet) {
        var opphører = finnOpphør(a, summertRefusjonTidslinje);
        var startDato = a.getStartdatoRefusjon().filter(d -> !d.isBefore(stp)).orElse(stp);

        var kanalReferansePrefiks = finnKanalReferansePrefiks(stp, inntektsmeldingerForAktivitet);
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medInnsendingstidspunkt(stp.atStartOfDay())
            .medArbeidsgiver(a.getArbeidsgiver())
            .medStartDatoPermisjon(stp)
            .medRefusjon(finnRefusjonVedStp(stp, summertRefusjonTidslinje, a, startDato), opphører)
            .medBeløp(a.getInntektPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medJournalpostId("OVERSTYRT_FOR_INFOTRYGDMIGRERING" + stp)
            .medKanalreferanse(kanalReferansePrefiks + "OVERSTYRT_FOR_INFOTRYGDMIGRERING" + stp);
        mapEndringer(stp, summertRefusjonTidslinje, startDato, opphører, inntektsmeldingBuilder);
        return inntektsmeldingBuilder
            .build();
    }

    /**
     * Finner største kanalreferansen blant liste med aktiviteter. Prefikses kanalreferansen for den overstyrte inntektsmeldingen med dette sørger vi for at det er den overstyrte inntektsmeldingen som velges.
     *
     * @param stp                           Skjæringstidspunkt
     * @param inntektsmeldingerForAktivitet Inntektsmeldinger for samme arbeidsgiver som aktuell overstyrt aktivitet
     * @return
     */
    private static String finnKanalReferansePrefiks(LocalDate stp, Collection<Inntektsmelding> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet.stream().filter(im -> im.getStartDatoPermisjon().isPresent() && im.getStartDatoPermisjon().get().equals(stp))
            .max(Comparator.comparing(Inntektsmelding::getKanalreferanse, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(Inntektsmelding::getKanalreferanse)
            .orElse("");
    }

    private static void mapEndringer(LocalDate stp,
                                     LocalDateTimeline<BigDecimal> summertRefusjonTidslinje,
                                     LocalDate startDato,
                                     LocalDate opphører,
                                     InntektsmeldingBuilder inntektsmeldingBuilder) {
        summertRefusjonTidslinje.toSegments()
            .stream()
            .filter(di -> di.getTom().isAfter(startDato) && (opphører == null || di.getFom().isBefore(opphører.plusDays(1))))
            .filter(s -> getFom(startDato, s).isAfter(stp))
            .forEach(s -> inntektsmeldingBuilder.leggTil(new Refusjon(s.getValue(), getFom(startDato, s))));
    }

    private static LocalDate getFom(LocalDate startDato, LocalDateSegment<BigDecimal> s) {
        return s.getFom().isBefore(startDato) ? startDato : s.getFom();
    }

    private static LocalDate finnOpphør(InputAktivitetOverstyring a, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje) {
        if (summertRefusjonTidslinje.isEmpty()) {
            return a.getOpphørRefusjon();
        }
        var opphørFraInntektsmelding = summertRefusjonTidslinje.toSegments().stream()
            .filter(s -> s.getTom().equals(TIDENES_ENDE) && s.getValue().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .map(LocalDateSegment::getFom);
        return opphørFraInntektsmelding.map(d -> d.minusDays(1)).orElse(null);
    }

    private static BigDecimal finnRefusjonVedStp(LocalDate stp,
                                                 LocalDateTimeline<BigDecimal> summertRefusjonTidslinje,
                                                 InputAktivitetOverstyring a,
                                                 LocalDate startDato) {
        if (startDato.isAfter(stp)) {
            return BigDecimal.ZERO;
        }
        var stpOverlapp = summertRefusjonTidslinje.getSegment(new LocalDateInterval(stp, stp));
        if (stpOverlapp == null) {
            return a.getRefusjonPrÅr() == null ? BigDecimal.ZERO :
                a.getRefusjonPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        }
        return stpOverlapp.getValue();
    }

    private static LocalDateTimeline<BigDecimal> tilRefusjontidslinje(Inntektsmelding im, LocalDate startdatoRefusjon) {
        ArrayList<LocalDateSegment<BigDecimal>> alleSegmenter = new ArrayList<>();
        var opphørsdatoRefusjon = im.getRefusjonOpphører();
        if (refusjonOpphørerFørStart(startdatoRefusjon, opphørsdatoRefusjon)) {
            return LocalDateTimeline.empty();
        }
        // Refusjon fra start
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        // Opphør
        if (opphørsdatoRefusjon != null) {
            alleSegmenter.add(new LocalDateSegment<>(opphørsdatoRefusjon.plusDays(1), TIDENES_ENDE, BigDecimal.ZERO));
        }

        // Endringer i mellom
        alleSegmenter.addAll(im.getEndringerRefusjon().stream()
            .map(e ->
                new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
            ).collect(Collectors.toList()));

        return new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });

    }

    private static boolean refusjonOpphørerFørStart(LocalDate startdatoRefusjon, LocalDate refusjonOpphører) {
        return refusjonOpphører != null && refusjonOpphører.isBefore(startdatoRefusjon);
    }

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

}
