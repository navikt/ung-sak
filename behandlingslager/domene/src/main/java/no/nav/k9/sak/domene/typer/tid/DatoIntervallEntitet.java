package no.nav.k9.sak.domene.typer.tid;

import java.time.LocalDate;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.vladmihalcea.hibernate.type.range.Range;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.typer.Periode;

/**
 * Hibernate entitet som modellerer et dato intervall.
 */
@Embeddable
public class DatoIntervallEntitet extends AbstractLocalDateInterval {

    @Column(name = "fom")
    private LocalDate fomDato;

    @Column(name = "tom")
    private LocalDate tomDato;

    private DatoIntervallEntitet() {
        // Hibernate
    }

    private DatoIntervallEntitet(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato == null && tomDato == null) {
            throw new IllegalArgumentException("Hverken fom (fra og med) dato, eller tom (til og med) dato er satt.");
        }
        if (fomDato == null) {
            throw new IllegalArgumentException("Fra og med dato må være satt. tom=" + tomDato);
        }
        if (tomDato == null) {
            throw new IllegalArgumentException("Til og med dato må være satt. fom=" + fomDato);
        }
        if (tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato. Fikk [" + fomDato + "," + tomDato + "]");
        }
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public static DatoIntervallEntitet fraOgMedTilOgMed(LocalDate fomDato, LocalDate tomDato) {
        return new DatoIntervallEntitet(fomDato, tomDato);
    }

    public static DatoIntervallEntitet fraOgMed(LocalDate fomDato) {
        return new DatoIntervallEntitet(fomDato, TIDENES_ENDE);
    }

    public static DatoIntervallEntitet tilOgMed(LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(TIDENES_BEGYNNELSE, tom);
    }

    @Override
    public LocalDate getFomDato() {
        return fomDato;
    }

    @Override
    public LocalDate getTomDato() {
        return tomDato;
    }

    @Override
    protected DatoIntervallEntitet lagNyPeriode(LocalDate fomDato, LocalDate tomDato) {
        return fraOgMedTilOgMed(fomDato, tomDato);
    }

    public static DatoIntervallEntitet fra(Range<LocalDate> periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(
            periode.lower() == null ? DatoIntervallEntitet.TIDENES_BEGYNNELSE : periode.hasMask(Range.LOWER_EXCLUSIVE) ? periode.lower().plusDays(1) : periode.lower(),
            periode.upper() == null ? DatoIntervallEntitet.TIDENES_ENDE : periode.hasMask(Range.UPPER_EXCLUSIVE) ? periode.upper().minusDays(1) : periode.upper());
    }

    public Range<LocalDate> toRange() {
        var fom = fomDato == null || DatoIntervallEntitet.TIDENES_BEGYNNELSE.equals(fomDato) ? null : fomDato;
        var tom = tomDato == null || DatoIntervallEntitet.TIDENES_ENDE.equals(tomDato) ? null : tomDato;

        if (fom != null && tom != null) {
            return Range.closed(fom, tom);
        } else if (fom == null) {
            if (tom != null) {
                return Range.infiniteClosed(tom);
            } else {
                return Range.infinite(LocalDate.class);
            }
        } else {
            return Range.closedInfinite(fom);
        }
    }

    public static DatoIntervallEntitet fra(Periode periode) {
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();
        return fra(fom, tom);
    }

    private static DatoIntervallEntitet fra(LocalDate fom, LocalDate tom) {
        if (fom != null && tom != null) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        } else if (fom != null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        } else if (tom != null) {
            return DatoIntervallEntitet.tilOgMed(tom);
        } else {
            return DatoIntervallEntitet.fraOgMedTilOgMed(TIDENES_BEGYNNELSE, TIDENES_ENDE);
        }
    }

    public static DatoIntervallEntitet fra(LocalDateInterval ld) {
        LocalDate fom = ld.isOpenStart() ? null : ld.getFomDato();
        LocalDate tom = ld.isOpenEnd() ? null : ld.getTomDato();
        return DatoIntervallEntitet.fra(fom, tom);
    }

    public static DatoIntervallEntitet minmax(Collection<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return null;
        }
        var min = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElseThrow();
        var max = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElseThrow();
        return DatoIntervallEntitet.fraOgMedTilOgMed(min, max);
    }

    public LocalDateInterval toLocalDateInterval() {
        return new LocalDateInterval(fomDato, tomDato);
    }

    public Periode tilPeriode() {
        return new Periode(getFomDato(), getTomDato());
    }

}
