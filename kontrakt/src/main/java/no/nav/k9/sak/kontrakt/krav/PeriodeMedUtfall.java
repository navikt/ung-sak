package no.nav.k9.sak.kontrakt.krav;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PeriodeMedUtfall {

    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @Valid
    @JsonProperty("utfall")
    private Utfall utfall;

    @JsonCreator
    public PeriodeMedUtfall(@Valid @NotNull @JsonProperty("periode") Periode periode,
                            @Valid @JsonProperty(value = "utfall", required = true) @NotNull Utfall utfall) {
        this.periode = periode;
        this.utfall = utfall;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Utfall getUtfall() {
        return utfall;
    }
}
