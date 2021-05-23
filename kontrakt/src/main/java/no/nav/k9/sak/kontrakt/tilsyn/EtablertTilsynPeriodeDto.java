package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.Periode;

import javax.validation.Valid;
import java.time.Duration;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EtablertTilsynPeriodeDto {

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "tidPerDag")
    @Valid
    private Duration tidPerDag;
    
    @JsonProperty(value = "kilde")
    @Valid
    private Kilde kilde;

    public EtablertTilsynPeriodeDto(Periode periode, Duration tidPerDag, Kilde kilde) {
        this.periode = periode;
        this.tidPerDag = tidPerDag;
        this.kilde = kilde;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Duration getTidPerDag() {
        return tidPerDag;
    }
    
    public Kilde getKilde() {
        return kilde;
    }
}
