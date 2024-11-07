package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.grunnbeløp.GrunnbeløpTjeneste;

@Dependent
public class LagGrunnbeløpTidslinjeTjeneste {

    private GrunnbeløpTjeneste grunnbeløpTjeneste;

    @Inject
    public LagGrunnbeløpTidslinjeTjeneste(GrunnbeløpTjeneste grunnbeløpTjeneste) {
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
    }

    /**
     * Lag grunnbeløptidslinje for perioder
     *
     * @param tidslinjeTilVurdering Perioder som skal lages tidslinje for
     * @return Tidslinje med grunnbeløp
     */
    public LocalDateTimeline<BigDecimal> lagGrunnbeløpTidslinjeForPeriode(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var grunnbeløpsdatoer = finnGrunnbeløpsdatoer(tidslinjeTilVurdering);

        var grunnbeløpTidslinje = grunnbeløpsdatoer.stream()
            .map(d -> grunnbeløpTjeneste.hentGrunnbeløp(d))
            .map(g -> new LocalDateTimeline<>(g.getPeriode().getFomDato(), g.getPeriode().getTomDato(), BigDecimal.valueOf(g.getVerdi())))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
        if (grunnbeløpTidslinje.isEmpty()) {
            throw new IllegalStateException("Grunnbeløpstidslinjen var tom. Dette skal ikke skje.");
        }
        return grunnbeløpTidslinje.intersection(tidslinjeTilVurdering);
    }

    /**
     * Finner datoer der vi skal spørre om grunnbeløp. Antar endring av g-verdi 1. mai hvert år.
     *
     * @param periodeTidslinje Perioder som skal vurderes
     * @return Datoer for endring av grunnbeløp i periode
     */
    // TODO: det er mer robust å spørre om tidslinje med G-verdier for hele perioden (i tilfelle det skulle endres på andre datoer)
    private static Set<LocalDate> finnGrunnbeløpsdatoer(LocalDateTimeline<Boolean> periodeTidslinje) {
        var grunnbeløpsdatoer = new HashSet<LocalDate>();
        periodeTidslinje.toSegments().forEach(p -> {
            if (p.getFom().isAfter(LocalDate.now())) {
                grunnbeløpsdatoer.add(LocalDate.now());
            } else {
                var startDato = p.getFom().getMonthValue() < 5 ? p.getFom().withYear(p.getFom().getYear() - 1).withMonth(5).withDayOfMonth(1) : p.getFom().withMonth(5).withDayOfMonth(1);
                var sluttDato = p.getTom().isBefore(LocalDate.now()) ? p.getTom() : LocalDate.now();
                while (!startDato.isAfter(sluttDato)) {
                    grunnbeløpsdatoer.add(startDato);
                    startDato = startDato.plusYears(1);
                }

            }
        });
        return grunnbeløpsdatoer;
    }


}
