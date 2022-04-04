package no.nav.k9.sak.kontrakt.krav;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrsakMedPerioder {

    @Valid
    @NotNull
    @JsonProperty("årsak")
    private ÅrsakTilVurdering årsak;

    @Valid
    @NotNull
    @Size
    @JsonProperty("perioder")
    private NavigableSet<Periode> perioder;

    public ÅrsakMedPerioder(@Valid @NotNull @JsonProperty("årsak") ÅrsakTilVurdering årsak, @Valid @NotNull @Size @JsonProperty("perioder") Set<Periode> perioder) {
        this.årsak = årsak;
        this.perioder = new TreeSet<>(perioder);
    }

    public ÅrsakTilVurdering getÅrsak() {
        return årsak;
    }

    public NavigableSet<Periode> getPerioder() {
        return perioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ÅrsakMedPerioder that = (ÅrsakMedPerioder) o;
        return årsak == that.årsak && Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(årsak, perioder);
    }

    @Override
    public String toString() {
        return "ÅrsakMedPerioder{" +
            "årsakTilVurdering=" + årsak +
            ", perioder=" + perioder +
            '}';
    }
}
