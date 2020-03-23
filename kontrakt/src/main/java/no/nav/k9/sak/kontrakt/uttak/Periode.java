package no.nav.k9.sak.kontrakt.uttak;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(value = Include.NON_ABSENT, content = Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Periode implements Comparable<Periode>{

    @JsonValue
    @NotNull
    @Size(max = 10 + 1 + 10)
    @Pattern(regexp = "^[\\p{Alnum}:\\-/]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Valid
    private String iso8601Periode;

    @JsonIgnore
    private LocalDate fom;

    @JsonIgnore
    private LocalDate tom;

    @JsonCreator
    public Periode(@NotNull @Size(max = 10 + 1 + 10) String iso8601Periode) {
        this.iso8601Periode = Objects.requireNonNull(iso8601Periode, "iso8601Periode");
        String[] strings = iso8601Periode.split("/");
        this.fom = LocalDate.parse(strings[0]);
        this.tom = LocalDate.parse(strings[1]);
    }

    public Periode(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("fom (fra-og-med dato) kan ikke være etter tom (til-og-med dato: " + fom + ">" + tom);
        }
        this.fom = fom;
        this.tom = tom;
        this.iso8601Periode = fom.toString() + "/" + tom.toString();
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
    
    private static final Comparator<Periode> COMP = Comparator
            .comparing((Periode dto) -> dto.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getTom(), Comparator.nullsFirst(Comparator.naturalOrder()));
}
