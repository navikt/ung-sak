package no.nav.k9.sak.kontrakt.vilkår;

import java.util.Properties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VilkårPeriodeDto {

    @JsonProperty(value = "avslagKode")
    @Size(max = 20)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String avslagKode;

    @JsonProperty(value = "evaluering", access = JsonProperty.Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String evaluering;

    @JsonProperty(value = "input", access = JsonProperty.Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String input;

    @JsonProperty(value = "merknadParametere")
    @Size(max = 20)
    private Properties merknadParametere;

    @JsonProperty(value = "vilkarStatus", required = true)
    @NotNull
    @Valid
    private Utfall vilkarStatus;

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    public VilkårPeriodeDto() {
    }

    public String getAvslagKode() {
        return avslagKode;
    }

    public void setAvslagKode(String avslagKode) {
        this.avslagKode = avslagKode;
    }

    public String getEvaluering() {
        return evaluering;
    }

    public void setEvaluering(String evaluering) {
        this.evaluering = evaluering;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Properties getMerknadParametere() {
        return merknadParametere;
    }

    public void setMerknadParametere(Properties merknadParametere) {
        this.merknadParametere = merknadParametere;
    }

    public Utfall getVilkarStatus() {
        return vilkarStatus;
    }

    public void setVilkarStatus(Utfall vilkarStatus) {
        this.vilkarStatus = vilkarStatus;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }
}
