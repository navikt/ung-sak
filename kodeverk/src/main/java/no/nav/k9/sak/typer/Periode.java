package no.nav.k9.sak.typer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "fom", "tom" })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Periode implements Comparable<Periode> {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    @Valid
    private LocalDate tom;

    Periode() {
        //
    }

    public Periode(YearMonth month) {
        Objects.requireNonNull(month);
        this.fom = month.atDay(1);
        this.tom = month.atEndOfMonth();
    }

    public Periode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
        validerOk();
    }

    public Periode(String iso8601) {
        verifiserKanVæreGyldigPeriode(iso8601);
        String[] split = iso8601.split("/");
        this.fom = parseLocalDate(split[0]);
        this.tom = parseLocalDate(split[1]);
        validerOk();
    }

    private void validerOk() {
        if (!ok()) {
            throw new IllegalArgumentException("fom (fra-og-med dato) kan ikke være etter tom (til-og-med dato: " + fom + ">" + tom);
        }
    }

    @AssertTrue(message = "fom dato må være <= tom dato hvis satt")
    private boolean ok() {
        LocalDate fom = getFom();
        LocalDate tom = getTom();
        return (fom == null || tom == null) || fom.isEqual(tom) || fom.isBefore(tom);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean overlaps(Periode other) {
        boolean starterFørEllerSamtidigSomAnnenPeriodeSlutter = (fom == null)
            || (fom != null && other.getTom() == null)
            || ((fom != null && other.getTom() != null)
                && (fom.isEqual(other.getTom()) || fom.isBefore(other.getTom())));

        if (starterFørEllerSamtidigSomAnnenPeriodeSlutter) {
            boolean slutterEtterEllerSamtidigSomPeriodeStarter = (tom == null)
                || (tom != null && other.getFom() == null)
                || ((tom != null && other.getFom() != null)
                    && (tom.isEqual(other.getFom()) || tom.isAfter(other.getFom())));
            return slutterEtterEllerSamtidigSomPeriodeStarter;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Periode)) {
            return false;
        }
        var annen = (Periode) o;
        return Objects.equals(fom, annen.getFom()) && Objects.equals(tom, annen.getTom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<fom=" + getFom() + ", tom=" + getTom() + ">";
    }

    @Override
    public int compareTo(Periode o) {
        return COMP.compare(this, o);
    }

    private static void verifiserKanVæreGyldigPeriode(String iso8601) {
        if (iso8601 == null || iso8601.split("/").length != 2) {
            throw new IllegalArgumentException("Periode på ugylig format '" + iso8601 + "'.");
        }
    }

    private static LocalDate parseLocalDate(String iso8601) {
        if ("..".equals(iso8601))
            return null;
        else
            return LocalDate.parse(iso8601);
    }

    private static final Comparator<Periode> COMP = Comparator
        .comparing((Periode dto) -> dto.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getTom(), Comparator.nullsLast(Comparator.naturalOrder()));
}
