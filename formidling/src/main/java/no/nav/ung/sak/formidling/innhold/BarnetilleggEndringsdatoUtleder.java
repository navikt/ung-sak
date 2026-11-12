package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.time.LocalDate;

public final class BarnetilleggEndringsdatoUtleder {

    public static final String TOGGLE_FØDSEL_FØR_STARTDATO = "BARNETILLEGG_BREV_FODSEL_FOR_STARTDATO_ENABLED";

    private BarnetilleggEndringsdatoUtleder() {
    }

    /**
     * Finner datoen barnetillegget gjelder fra. Barnet kan være født før startdato, og da starter
     * fødselsperioden før satstidslinjen. Barnetillegget gjelder da fra første dag med sats.
     */
    public static LocalDate finnFørsteDatoMedSats(LocalDateTimeline<?> fødselTidslinje, LocalDateTimeline<?> satsTidslinje) {
        var fødselMedSats = satsTidslinje.intersection(fødselTidslinje);
        if (fødselMedSats.isEmpty()) {
            throw new IllegalStateException("Fant ingen sats i perioden for fødsel " + fødselTidslinje.getLocalDateIntervals());
        }
        return fødselMedSats.getMinLocalDate();
    }
}
