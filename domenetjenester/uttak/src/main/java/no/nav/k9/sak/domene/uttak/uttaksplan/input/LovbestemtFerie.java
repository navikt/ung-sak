package no.nav.k9.sak.domene.uttak.uttaksplan.input;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.sak.kontrakt.uttak.Periode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class LovbestemtFerie implements Comparable<LovbestemtFerie> {

    @JsonValue
    @Valid
    @NotNull
    private Periode periode;

    @JsonCreator
    public LovbestemtFerie(Periode periode) {
        this.periode = Objects.requireNonNull(periode, "periode");
    }

    public Periode getPeriode() {
        return periode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + periode + ">";
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof LovbestemtFerie))
            return false;
        var other = (LovbestemtFerie) obj;
        return Objects.equals(this.periode, other.periode);
    }

    @Override
    public int compareTo(LovbestemtFerie o) {
        return this.periode.compareTo(o.periode);
    }
}
