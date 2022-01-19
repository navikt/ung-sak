package no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.uttak.Periode;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsPeriode {

    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @JsonProperty("fortsett")
    private Boolean kanFortsette;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonCreator
    public KompletthetsPeriode(@Valid @NotNull @JsonProperty(value = "periode", required = true) Periode periode,
                               @NotNull @JsonProperty(value = "fortsett", required = true) Boolean kanFortsette,
                               @JsonProperty("begrunnelse")
                               @Size(max = 4000)
                               @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse) {
        this.periode = periode;
        this.kanFortsette = kanFortsette;
        this.begrunnelse = begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getKanFortsette() {
        return kanFortsette;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
