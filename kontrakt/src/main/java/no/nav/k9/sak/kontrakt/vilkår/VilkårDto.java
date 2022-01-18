package no.nav.k9.sak.kontrakt.vilkår;

import java.util.Objects;
import java.util.Properties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonRawValue;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VilkårDto {

    @JsonProperty(value = "avslagKode")
    @Size(max = 20)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String avslagKode;

    @JsonProperty(value = "evaluering", access = Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String evaluering;

    @JsonProperty(value = "input", access = Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String input;

    @JsonProperty(value = "lovReferanse")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{P}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String lovReferanse;

    @JsonProperty(value = "merknadParametere")
    @Size(max = 20)
    private Properties merknadParametere;

    @JsonProperty(value = "overstyrbar")
    private Boolean overstyrbar;

    @JsonProperty(value = "vilkarStatus", required = true)
    @NotNull
    @Valid
    private Utfall vilkarStatus;

    @JsonProperty(value = "vilkarType", required = true)
    @NotNull
    @Valid
    private VilkårType vilkarType;

    public VilkårDto() {
    }

    public VilkårDto(VilkårType vilkårType,
                     Utfall utfall,
                     Properties merknadParametere,
                     String avslagKode,
                     String lovReferanse) {
        this.vilkarType = Objects.requireNonNull(vilkårType, "vilkårType");
        this.vilkarStatus = utfall;
        this.merknadParametere = merknadParametere;
        this.avslagKode = avslagKode;
        this.lovReferanse = lovReferanse;
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

    public String getLovReferanse() {
        return lovReferanse;
    }

    public void setLovReferanse(String lovReferanse) {
        this.lovReferanse = lovReferanse;
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

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public boolean isOverstyrbar() {
        return overstyrbar;
    }

    public void setOverstyrbar(boolean overstyrbar) {
        this.overstyrbar = overstyrbar;
    }
}
