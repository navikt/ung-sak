package no.nav.k9.sak.kontrakt.tilsyn;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SmørtEtablertTilsyn {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "tidPerDag")
    @Valid
    private Duration tidPerDag;

    public SmørtEtablertTilsyn(Periode periode, Duration tidPerDag) {
        this.periode = periode;
        this.tidPerDag = tidPerDag;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Duration getTidPerDag() {
        return tidPerDag;
    }

}
