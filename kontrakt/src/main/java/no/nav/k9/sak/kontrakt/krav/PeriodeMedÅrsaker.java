package no.nav.k9.sak.kontrakt.krav;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
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
public class PeriodeMedÅrsaker {

    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @Valid
    @Size
    @JsonProperty("årsaker")
    private Set<ÅrsakTilVurdering> årsaker;

    @JsonCreator
    public PeriodeMedÅrsaker(@Valid @NotNull @JsonProperty("periode") Periode periode,
                             @Valid @Size @JsonProperty("årsaker") Set<ÅrsakTilVurdering> årsaker) {
        this.periode = periode;
        this.årsaker = årsaker;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Set<ÅrsakTilVurdering> getÅrsaker() {
        return årsaker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodeMedÅrsaker that = (PeriodeMedÅrsaker) o;
        return Objects.equals(periode, that.periode) && Objects.equals(årsaker, that.årsaker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, årsaker);
    }
}
