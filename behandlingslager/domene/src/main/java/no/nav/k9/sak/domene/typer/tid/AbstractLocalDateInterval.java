package no.nav.k9.sak.domene.typer.tid;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.threeten.extra.Interval;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;

/**
 * Basis klasse for modellere et dato interval.
 */
public abstract class AbstractLocalDateInterval implements Comparable<AbstractLocalDateInterval>, Serializable {

    public static final LocalDate TIDENES_BEGYNNELSE = Tid.TIDENES_BEGYNNELSE;
    public static final LocalDate TIDENES_ENDE = Tid.TIDENES_ENDE;

    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static Interval getIntervall(LocalDate fomDato, LocalDate tomDato) {
        LocalDateTime døgnstart = (TIDENES_ENDE.equals(tomDato) || TIDENES_ENDE.isBefore(tomDato)) ? TIDENES_ENDE.atStartOfDay() : tomDato.atStartOfDay().plusDays(1);
        return Interval.of(
            fomDato.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
            døgnstart.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static List<LocalDate> listArbeidsdager(LocalDate fomDato, LocalDate tomDato) { // NOSONAR
        List<LocalDate> arbeidsdager = new ArrayList<>();
        LocalDate dato = fomDato;
        while (!dato.isAfter(tomDato)) {
            if (erArbeidsdag(dato)) {
                arbeidsdager.add(dato);
            }
            dato = dato.plusDays(1L);
        }
        return arbeidsdager;
    }

    protected static boolean erArbeidsdag(LocalDate dato) {
        return !dato.getDayOfWeek().equals(SATURDAY) && !dato.getDayOfWeek().equals(SUNDAY); // NOSONAR
    }

    public abstract LocalDate getFomDato();

    public abstract LocalDate getTomDato();

    protected abstract AbstractLocalDateInterval lagNyPeriode(LocalDate fomDato, LocalDate tomDato);

    public Interval tilIntervall() {
        return getIntervall(getFomDato(), getTomDato());
    }

    private boolean erFørEllerLikPeriodeslutt(ChronoLocalDate dato) {
        return getTomDato() == null || getTomDato().isAfter(dato) || getTomDato().isEqual(dato);
    }

    private boolean erEtterEllerLikPeriodestart(ChronoLocalDate dato) {
        return getFomDato().isBefore(dato) || getFomDato().isEqual(dato);
    }

    public boolean inkluderer(ChronoLocalDate dato) {
        return erEtterEllerLikPeriodestart(dato) && erFørEllerLikPeriodeslutt(dato);
    }

    public boolean overlapper(AbstractLocalDateInterval periode) {
        return tilIntervall().overlaps(getIntervall(periode.getFomDato(), periode.getTomDato()));
    }

    public boolean overlapper(LocalDate fom, LocalDate tom) {
        return tilIntervall().overlaps(getIntervall(fom, tom));
    }

    public int antallArbeidsdager() {
        if (getTomDato().isEqual(TIDENES_ENDE)) {
            throw new IllegalStateException("Både fra og med og til og med dato må være satt for å regne ut arbeidsdager.");
        }
        return arbeidsdager().size();
    }

    public List<LocalDate> arbeidsdager() {
        return listArbeidsdager(getFomDato(), getTomDato());
    }

    public boolean grenserTil(AbstractLocalDateInterval periode2) {
        return getTomDato().equals(periode2.getFomDato().minusDays(1)) || periode2.getTomDato().equals(getFomDato().minusDays(1));
    }

    @Override
    public int compareTo(AbstractLocalDateInterval periode) {
        return getFomDato().compareTo(periode.getFomDato());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var annen = (AbstractLocalDateInterval) obj;
        return Objects.equals(this.getFomDato(), annen.getFomDato())
            && Objects.equals(this.getTomDato(), annen.getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFomDato(), getTomDato());
    }

    @Override
    public String toString() {
        if (getFomDato().equals(TIDENES_BEGYNNELSE) && getTomDato().equals(TIDENES_ENDE)) {
            return "Periode: (,)";
        } else if (getFomDato().equals(TIDENES_BEGYNNELSE)) {
            return String.format("Periode: (,%s]", getTomDato().format(FORMATTER));
        } else if (getTomDato().equals(TIDENES_ENDE)) {
            return String.format("Periode: [%s,)", getFomDato().format(FORMATTER));
        } else {
            return String.format("Periode: [%s,%s]", getFomDato().format(FORMATTER), getTomDato().format(FORMATTER));
        }
    }
}
