package no.nav.k9.sak.kontrakt.vilkår;



import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VilkårPeriodeVurderingDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @JsonProperty("avslagskode")
    @Size(min = 4, max = 5)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String avslagskode;

    @JsonProperty(value = "innvilgelseMerknadKode")
    @Size(min = 4, max = 5)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String innvilgelseMerknadKode;

    public VilkårPeriodeVurderingDto() {
    }

    public VilkårPeriodeVurderingDto(Periode periode, boolean erVilkarOk, String avslagskode, String innvilgelseMerknadKode) {
        this.periode = periode;
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
        this.innvilgelseMerknadKode = innvilgelseMerknadKode;
    }

    public Periode getPeriode() {
        return periode;
    }

    public boolean isErVilkarOk() {
        return erVilkarOk;
    }

    public String getAvslagskode() {
        return avslagskode;
    }

    public String getInnvilgelseMerknadKode() {
        return innvilgelseMerknadKode;
    }
}
