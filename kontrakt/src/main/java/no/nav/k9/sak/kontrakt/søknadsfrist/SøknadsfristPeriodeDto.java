package no.nav.k9.sak.kontrakt.søknadsfrist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SøknadsfristPeriodeDto {

    @NotNull
    @Valid
    @JsonProperty("periode")
    private Periode periode;

    @Valid
    @JsonProperty(value = "status")
    private Utfall status;

    @JsonCreator
    public SøknadsfristPeriodeDto(@JsonProperty("periode") Periode periode,
                                  @JsonProperty("status") Utfall status) {
        this.periode = periode;
        this.status = status;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Utfall getStatus() {
        return status;
    }
}
