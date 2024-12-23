package no.nav.ung.sak.behandlingslager.aktør.historikk;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Objects;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;

public class Gyldighetsperiode {
    private LocalDate fom;
    private LocalDate tom;

    private static final LocalDate FREG_TIDENES_BEGYNNELSE = LocalDate.of(0, Month.DECEMBER, 30);

    private Gyldighetsperiode(LocalDate fom, LocalDate tom) {
        // Fom er null om perioden for en opplysning gjelder fra personen ble født
        // Setter da fom til tidenes begynnelse for å slippe et ekstra kall for å hente fødselsdato
        if (fom == null) {
            fom = Tid.TIDENES_BEGYNNELSE;
        }
        if (tom == null) {
            tom = Tid.TIDENES_ENDE;
        }

        this.fom = fom;
        this.tom = tom;
    }

    public static Gyldighetsperiode innenfor(LocalDate fom, LocalDate tom) {
        return new Gyldighetsperiode(fom, tom);
    }

    public static Gyldighetsperiode fraTilTidenesEnde(LocalDate fom) {
        return new Gyldighetsperiode(fom, Tid.TIDENES_ENDE);
    }

    public static boolean fuzzyEquals(Gyldighetsperiode p1, Gyldighetsperiode p2) {
        var fuzzyfom = Objects.equals(p1.fom, Tid.TIDENES_BEGYNNELSE) || Objects.equals(p2.fom, Tid.TIDENES_BEGYNNELSE) ||
            Objects.equals(p1.fom, FREG_TIDENES_BEGYNNELSE) || Objects.equals(p2.fom, FREG_TIDENES_BEGYNNELSE) ||
            Math.abs(Period.between(p1.fom, p2.fom).getDays()) < 21;
        var fuzzytom = Objects.equals(p1.tom, p2.tom) || Math.abs(Period.between(p1.tom, p2.tom).getDays()) < 21;
        return fuzzyfom && fuzzytom;
    }

    public LocalDate getFom() {
        return this.fom;
    }

    public LocalDate getTom() {
        return this.tom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gyldighetsperiode that = (Gyldighetsperiode) o;
        return Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Gyldighetsperiode{");
        sb.append("fom=").append(fom);
        sb.append(", tom=").append(tom);
        sb.append('}');
        return sb.toString();
    }
}
