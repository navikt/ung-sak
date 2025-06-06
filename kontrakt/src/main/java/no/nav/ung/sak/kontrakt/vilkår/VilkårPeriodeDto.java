package no.nav.ung.sak.kontrakt.vilkår;

import java.util.Properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VilkårPeriodeDto {

    @JsonProperty(value = "avslagKode")
    @Size(max = 20)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String avslagKode;

    @JsonProperty(value = "evaluering", access = JsonProperty.Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String evaluering;

    @JsonProperty(value = "input", access = JsonProperty.Access.READ_ONLY)
    @JsonRawValue
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 1000000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
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

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    /**
     * indikerer om en vilkårsperiode er aktuell for vurdering i inneværende behandling
     */
    @JsonProperty("vurderesIBehandlingen")
    private Boolean vurderesIBehandlingen;

    @JsonProperty(value = "merknad")
    @Valid
    private VilkårUtfallMerknad merknad;

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

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }


    public Boolean getVurderesIBehandlingen() {
        return vurderesIBehandlingen;
    }

    public void setVurderesIBehandlingen(Boolean vurderesIBehandlingen) {
        this.vurderesIBehandlingen = vurderesIBehandlingen;
    }

    public VilkårUtfallMerknad getMerknad() {
        return merknad;
    }

    public void setMerknad(VilkårUtfallMerknad merknad) {
        this.merknad = merknad;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<periode=" + periode
            + ", vilkarStatus=" + vilkarStatus
            + (avslagKode == null ? "" : ", avslagKode=" + avslagKode)
            + (begrunnelse == null ? "" : ", begrunnelse=" + begrunnelse)
            + (merknadParametere == null ? "" : ", merknadParametere=" + merknadParametere)
            + (vurderesIBehandlingen == null ? "" : ", vurderesIBehandlingen=" + vurderesIBehandlingen)
            + ">";
    }
}
