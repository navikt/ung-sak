package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Stillingsprosent;

public class Gradering implements IndexKey, Comparable<Gradering> {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private Stillingsprosent arbeidstidProsent;

    Gradering() {
    }

    public Gradering(DatoIntervallEntitet periode, BigDecimal arbeidstidProsent) {
        this.arbeidstidProsent = new Stillingsprosent(Objects.requireNonNull(arbeidstidProsent,  "arbeidstidProsent"));
        this.periode = periode;
    }

    public Gradering(DatoIntervallEntitet periode, Stillingsprosent arbeidstidProsent) {
        this.arbeidstidProsent = arbeidstidProsent;
        this.periode = periode;
    }

    public Gradering(LocalDate fom, LocalDate tom, BigDecimal arbeidstidProsent) {
        this(tom == null ? DatoIntervallEntitet.fraOgMed(fom) : DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), new Stillingsprosent(Objects.requireNonNull(arbeidstidProsent,  "arbeidstidProsent")));
    }

    Gradering(Gradering gradering) {
        this(gradering.getPeriode(), gradering.getArbeidstidProsent());
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode };
        return IndexKeyComposer.createKey(keyParts);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    /**
     * En arbeidstaker kan kombinere foreldrepenger med deltidsarbeid.
     *
     * Når arbeidstakeren jobber deltid, utgjør foreldrepengene differansen mellom deltidsarbeidet og en 100 prosent stilling.
     * Det er ingen nedre eller øvre grense for hvor mye eller lite arbeidstakeren kan arbeide.
     *
     * Eksempel
     * Arbeidstaker A har en 100 % stilling og arbeider fem dager i uken. Arbeidstakeren ønsker å arbeide to dager i uken i
     * foreldrepengeperioden.
     * Arbeidstids- prosenten blir da 40 %.
     *
     * Arbeidstaker B har en 80 % stilling og arbeider fire dager i uken. Arbeidstakeren ønsker å arbeide to dager i uken i
     * foreldrepengeperioden.
     * Arbeidstidprosenten blir også her 40 %.
     *
     * @return prosentsats
     */
    public Stillingsprosent getArbeidstidProsent() {
        return arbeidstidProsent;
    }

    @Override
    public String toString() {
        return "GraderingEntitet{" +
            "periode=" + periode +
            ", arbeidstidProsent=" + arbeidstidProsent +
            '}';
    }

    @Override
    public int compareTo(Gradering o) {
        return o == null ? 1 : this.getPeriode().compareTo(o.getPeriode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Gradering)) return false;
        Gradering that = (Gradering) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }
}
