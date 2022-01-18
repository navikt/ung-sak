package no.nav.k9.sak.kontrakt.krav;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
