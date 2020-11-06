package no.nav.k9.sak.domene.typer.tid;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.vladmihalcea.hibernate.type.range.Range;

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
        if (fomDato == null) {
            throw new IllegalArgumentException("Fra og med dato må være satt.");
        }
        if (tomDato == null) {
            throw new IllegalArgumentException("Til og med dato må være satt.");
        }
        if (tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato.");
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

    public static DatoIntervallEntitet fraOgMedPlusArbeidsdager(LocalDate fom, int antallArbeidsdager) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, finnTomDato(fom, antallArbeidsdager));
    }

    public static DatoIntervallEntitet tilOgMedMinusArbeidsdager(LocalDate tom, int antallArbeidsdager) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(finnFomDato(tom, antallArbeidsdager), tom);
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
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

}
