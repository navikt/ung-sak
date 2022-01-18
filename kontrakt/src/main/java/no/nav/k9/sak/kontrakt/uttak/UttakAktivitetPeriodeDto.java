package no.nav.k9.sak.kontrakt.uttak;

import java.util.Comparator;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class UttakAktivitetPeriodeDto implements Comparable<UttakAktivitetPeriodeDto> {

    private static final Comparator<UttakAktivitetPeriodeDto> COMP = Comparator
        .comparing(UttakAktivitetPeriodeDto::getPeriode, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(UttakAktivitetPeriodeDto::getType, Comparator.nullsLast(Comparator.naturalOrder()));

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "type", required = true)
    @Valid
    @NotNull
    private UttakArbeidType type;

    @JsonCreator
    public UttakAktivitetPeriodeDto(@JsonProperty(value = "periode", required = true) @Valid @NotNull Periode periode,
                                    @JsonProperty(value = "type", required = true) @Valid @NotNull UttakArbeidType type) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.type = Objects.requireNonNull(type, "type");
    }

    public UttakArbeidType getType() {
        return type;
    }

    public Periode getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var other = (UttakAktivitetPeriodeDto) obj;
        return Objects.equals(periode, other.periode)
            && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + periode + ", type=" + type + ">";
    }

    @Override
    public int compareTo(UttakAktivitetPeriodeDto o) {
        if (this == o)
            return 0;
        return COMP.compare(this, o);
    }

}
