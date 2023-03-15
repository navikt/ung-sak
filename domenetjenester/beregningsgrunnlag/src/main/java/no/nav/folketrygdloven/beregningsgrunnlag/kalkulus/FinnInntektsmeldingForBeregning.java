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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
public class FinnInntektsmeldingForBeregning {

    private static final Logger logger = LoggerFactory.getLogger(FinnInntektsmeldingForBeregning.class);


    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private FiltrerInntektsmeldingForBeregningInputOverstyring finnForOverstyringTjeneste;


    public FinnInntektsmeldingForBeregning() {
    }

    @Inject
    public FinnInntektsmeldingForBeregning(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                           InntektArbeidYtelseTjeneste iayTjeneste, FiltrerInntektsmeldingForBeregningInputOverstyring finnForOverstyringTjeneste) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.iayTjeneste = iayTjeneste;
        this.finnForOverstyringTjeneste = finnForOverstyringTjeneste;
    }

    Set<Inntektsmelding> finnInntektsmeldinger(BehandlingReferanse referanse, List<BeregnInput> beregnInput) {
        var inntektsmeldingerForSak = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        Set<Inntektsmelding> overstyrteInntektsmeldinger = finnOverstyrteInntektsmeldinger(referanse, beregnInput, inntektsmeldingerForSak);
        var inntektsmeldinger = new HashSet<Inntektsmelding>();
        inntektsmeldinger.addAll(overstyrteInntektsmeldinger);
        inntektsmeldinger.addAll(inntektsmeldingerForSak);
        return inntektsmeldinger;
    }

    private Set<Inntektsmelding> finnOverstyrteInntektsmeldinger(BehandlingReferanse referanse, List<BeregnInput> beregnInput, Set<Inntektsmelding> inntektsmeldingerForSak) {
        return beregnInput.stream().flatMap(i -> {
            var relevanteInntektsmeldinger = finnForOverstyringTjeneste.finnGyldighetstidslinjeForInntektsmeldinger(referanse, inntektsmeldingerForSak, i.getVilkårsperiode());
            return i.getInputOverstyringPeriode().stream().flatMap(overstyrtPeriode -> mapInntektsmeldingerForOverstyrtPeriode(relevanteInntektsmeldinger, overstyrtPeriode));
        }).collect(Collectors.toSet());
    }

    private static Stream<Inntektsmelding> mapInntektsmeldingerForOverstyrtPeriode(LocalDateTimeline<Set<Inntektsmelding>> mottatteInntektsmeldingerForPeriode, InputOverstyringPeriode overstyrtPeriode) {
        LocalDate stp = overstyrtPeriode.getSkjæringstidspunkt();
        return overstyrtPeriode.getAktivitetOverstyringer().stream()
            .filter(a -> a.getAktivitetStatus().erArbeidstaker())
            .map(a -> mapAktivitetTilInntektsmelding(a, stp, mottatteInntektsmeldingerForPeriode));
    }

    static Inntektsmelding mapAktivitetTilInntektsmelding(InputAktivitetOverstyring a,
                                                          LocalDate stp,
                                                          LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForSak) {
        var inntektsmeldingerForAktivitet = finnInntektsmeldingerMottattForAktivitet(inntektsmeldingerForSak, a);
        var alleInntektsmeldinger = inntektsmeldingerForAktivitet.toSegments().stream()
            .flatMap(s -> s.getValue().stream())
            .collect(Collectors.toSet());
        logger.info("Overgang fra infotrygd: Mapper overstyring for aktivitet " + a
            + " Fant mottatte " + alleInntektsmeldinger.size() + " inntektsmeldinger for abrbeidsgiver: "
            + alleInntektsmeldinger.stream().map(Inntektsmelding::getJournalpostId).toList());

        var summertRefusjonTidslinje = lagSummertRefusjontidslinje(stp, inntektsmeldingerForAktivitet);

        return mapInntektsmelding(stp, a, summertRefusjonTidslinje, alleInntektsmeldinger);
    }

    public static LocalDateTimeline<Set<Inntektsmelding>> finnInntektsmeldingerMottattForAktivitet(LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForSak, InputAktivitetOverstyring a) {
        return inntektsmeldingerForSak.mapValue(ims -> ims.stream().filter(i -> a.getArbeidsgiver().equals(i.getArbeidsgiver())).collect(Collectors.toSet()));
    }

    public static LocalDateTimeline<BigDecimal> lagSummertRefusjontidslinje(LocalDate stp, LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet
            .map(ims -> ims.getValue().stream()
                .map(im -> tilRefusjontidslinje(im, stp))
                .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
                .orElse(LocalDateTimeline.empty())
                .intersection(ims.getLocalDateInterval())
                .stream()
                .toList())
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
        var refusjonsendringer = summertRefusjonTidslinje.toSegments()
            .stream()
            .filter(di -> di.getTom().isAfter(startDato) && (opphører == null || di.getFom().isBefore(opphører.plusDays(1))))
            .filter(s -> getFom(startDato, s).isAfter(stp))
            .map(s -> new Refusjon(s.getValue(), getFom(startDato, s)))
            .toList();

        logger.info("Mappet refusjonstidslinje " + summertRefusjonTidslinje +
            " til følgende endringer " + refusjonsendringer +
            " med startdato " + startDato +
            " og opphør " + opphører);

        refusjonsendringer.forEach(inntektsmeldingBuilder::leggTil);
    }

    private static LocalDate getFom(LocalDate startDato, LocalDateSegment<BigDecimal> s) {
        return s.getFom().isBefore(startDato) ? startDato : s.getFom();
    }

    private static LocalDate finnOpphør(InputAktivitetOverstyring a, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje) {
        if (summertRefusjonTidslinje.isEmpty()) {
            return a.getOpphørRefusjon();
        }
        return summertRefusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0)
            .getMaxLocalDate();
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
        if (opphørsdatoRefusjon != null && opphørsdatoRefusjon.isBefore(TIDENES_ENDE)) {
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

}
