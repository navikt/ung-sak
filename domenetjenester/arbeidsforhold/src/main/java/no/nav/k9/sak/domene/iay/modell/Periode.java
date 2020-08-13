package no.nav.k9.sak.domene.iay.modell;

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

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@JsonPropertyOrder({"fom", "tom"})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Periode implements Comparable<Periode> {

    private static final Comparator<Periode> COMP = Comparator
        .comparing((Periode dto) -> dto.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getTom(), Comparator.nullsFirst(Comparator.naturalOrder()));

    @ChangeTracked
    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;

    @ChangeTracked
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
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("fom (fra-og-med dato) kan ikke være etter tom (til-og-med dato: " + fom + ">" + tom);
        }
        this.fom = fom;
        this.tom = tom;
    }

    @AssertTrue(message = "fom dato må være <= tom dato")
    private boolean ok() {
        return getFom().isEqual(getTom()) || getFom().isBefore(getTom());
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean starterFørEllerSamtidigSom(Periode periode) {
        return fom.isEqual(periode.getFom()) || fom.isBefore(periode.getFom());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Periode)) {
            return false;
        }
        var annen = (Periode) o;
        return fom.equals(annen.getFom()) && tom.equals(annen.getTom());
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
}
