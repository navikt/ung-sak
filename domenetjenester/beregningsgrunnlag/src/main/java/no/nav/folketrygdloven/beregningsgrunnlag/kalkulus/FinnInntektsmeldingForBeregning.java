package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
public class FinnInntektsmeldingForBeregning {

    private static final Logger logger = LoggerFactory.getLogger(FinnInntektsmeldingForBeregning.class);


    private InntektArbeidYtelseTjeneste iayTjeneste;
    private FiltrerInntektsmeldingForBeregningInputOverstyring finnForOverstyringTjeneste;

    public FinnInntektsmeldingForBeregning() {
    }

    @Inject
    public FinnInntektsmeldingForBeregning(InntektArbeidYtelseTjeneste iayTjeneste, FiltrerInntektsmeldingForBeregningInputOverstyring finnForOverstyringTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.finnForOverstyringTjeneste = finnForOverstyringTjeneste;
    }

    public Set<Inntektsmelding> finnInntektsmeldinger(BehandlingReferanse referanse, List<BeregnInput> beregnInput) {
        var inntektsmeldingerForSak = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());
        Set<Inntektsmelding> overstyrteInntektsmeldinger = finnOverstyrteInntektsmeldinger(referanse, beregnInput, inntektsmeldingerForSak);
        var inntektsmeldinger = new HashSet<Inntektsmelding>();
        inntektsmeldinger.addAll(overstyrteInntektsmeldinger);
        inntektsmeldinger.addAll(inntektsmeldingerForSak);
        return inntektsmeldinger;
    }

    private Set<Inntektsmelding> finnOverstyrteInntektsmeldinger(BehandlingReferanse referanse, List<BeregnInput> beregnInput, Set<Inntektsmelding> inntektsmeldingerForSak) {
        return beregnInput.stream().flatMap(i -> {
            var inntektsmeldingTidslinje = finnForOverstyringTjeneste.finnGyldighetstidslinjeForInntektsmeldinger(referanse, inntektsmeldingerForSak, i.getVilkårsperiode());
            return i.getInputOverstyringPeriode().stream().flatMap(overstyrtPeriode -> mapInntektsmeldingerForOverstyrtPeriode(inntektsmeldingTidslinje, overstyrtPeriode));
        }).collect(Collectors.toSet());
    }

    private static Stream<Inntektsmelding> mapInntektsmeldingerForOverstyrtPeriode(LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingTidslinje, InputOverstyringPeriode overstyrtPeriode) {
        LocalDate stp = overstyrtPeriode.getSkjæringstidspunkt();
        return overstyrtPeriode.getAktivitetOverstyringer().stream()
            .filter(a -> a.getAktivitetStatus().erArbeidstaker())
            .map(a -> mapAktivitetTilInntektsmelding(a, stp, inntektsmeldingTidslinje))
            .filter(Objects::nonNull);
    }

    static Inntektsmelding mapAktivitetTilInntektsmelding(InputAktivitetOverstyring a,
                                                          LocalDate stp,
                                                          LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingTidslinje) {
        var inntektsmeldingerForAktivitet = finnInntektsmeldingerMottattForAktivitet(inntektsmeldingTidslinje, a);
        var alleInntektsmeldinger = finnAlleInntektsmeldingerForHelePerioden(inntektsmeldingerForAktivitet);

        logger.info("Overgang fra infotrygd: Mapper overstyring for aktivitet " + a
            + " Fant mottatte " + alleInntektsmeldinger.size() + " inntektsmeldinger for abrbeidsgiver: "
            + alleInntektsmeldinger.stream().map(Inntektsmelding::getJournalpostId).toList());

        var summertRefusjonTidslinje = SammenstillRefusjonskravForInfotrygdmigrering.lagTidslinje(stp, inntektsmeldingerForAktivitet);

        return mapInntektsmelding(stp, a, summertRefusjonTidslinje, alleInntektsmeldinger);
    }

    private static Set<Inntektsmelding> finnAlleInntektsmeldingerForHelePerioden(LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet.toSegments().stream()
            .flatMap(s -> s.getValue().stream())
            .collect(Collectors.toSet());
    }

    public static LocalDateTimeline<Set<Inntektsmelding>> finnInntektsmeldingerMottattForAktivitet(LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForSak, InputAktivitetOverstyring a) {
        return inntektsmeldingerForSak.mapValue(ims -> ims.stream().filter(i -> a.getArbeidsgiver().equals(i.getArbeidsgiver())).collect(Collectors.toSet()));
    }


    private static Inntektsmelding mapInntektsmelding(LocalDate stp, InputAktivitetOverstyring a,
                                                      LocalDateTimeline<BigDecimal> summertRefusjonTidslinje,
                                                      Collection<Inntektsmelding> inntektsmeldingerForAktivitet) {
        var opphører = finnOpphør(a, summertRefusjonTidslinje);
        var startDato = a.getStartdatoRefusjon().filter(d -> !d.isBefore(stp)).orElse(stp);
        var inntektBeløp = finnInntektsbeløp(a, inntektsmeldingerForAktivitet);
        if (inntektBeløp == null) {
            // Dersom vi ikke finner beløp hverken fra overstyringen eller fra eksisterende inntektsmeldinger,
            // så betyr det i praksis at ingenting er overstyrt, og vi trenger ikke lage en overstyrt inntektsmelding her
            return null;
        }

        // Hack for å sørge for at inntektsmeldingen velges blant inntektsmeldinger med samem skjæringstidspunkt
        var kanalReferansePrefiks = finnKanalReferansePrefiks(stp, inntektsmeldingerForAktivitet);
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medInnsendingstidspunkt(stp.atStartOfDay())
            .medArbeidsgiver(a.getArbeidsgiver())
            .medStartDatoPermisjon(stp)
            .medRefusjon(finnRefusjonVedStp(stp, summertRefusjonTidslinje, a, startDato), opphører)
            .medBeløp(inntektBeløp)
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medJournalpostId("OVERSTYRT_" + stp)
            .medKanalreferanse(kanalReferansePrefiks + "_OVERSTYRT_" + stp);
        mapEndringer(stp, summertRefusjonTidslinje, startDato, opphører, inntektsmeldingBuilder);
        return inntektsmeldingBuilder
            .build();
    }

    private static BigDecimal finnInntektsbeløp(InputAktivitetOverstyring a, Collection<Inntektsmelding> inntektsmeldingerForAktivitet) {
        if (a.getInntektPrÅr() != null) {
            return a.getInntektPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        }
        if (!inntektsmeldingerForAktivitet.isEmpty()) {
            return inntektsmeldingerForAktivitet.stream()
                .map(Inntektsmelding::getInntektBeløp)
                .reduce(Beløp::adder)
                .map(Beløp::getVerdi)
                .orElseThrow(() -> new IllegalStateException("Fant ingen inntektsmelding for aktivitet"));
        }
        return null;
    }

    /**
     * Finner største kanalreferansen blant liste med aktiviteter. Prefikses kanalreferansen for den overstyrte inntektsmeldingen med dette sørger vi for at det er den overstyrte inntektsmeldingen som velges.
     *
     * @param stp                           Skjæringstidspunkt
     * @param inntektsmeldingerForAktivitet Inntektsmeldinger for samme arbeidsgiver som aktuell overstyrt aktivitet
     * @return prefiks for kanalreferanse
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
        if (a.getOpphørRefusjon() != null) {
            return a.getOpphørRefusjon();
        }
        var refusjonTidslinje = summertRefusjonTidslinje.filterValue(r -> r.compareTo(BigDecimal.ZERO) > 0);
        return refusjonTidslinje.isEmpty() ? null : refusjonTidslinje
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


}
