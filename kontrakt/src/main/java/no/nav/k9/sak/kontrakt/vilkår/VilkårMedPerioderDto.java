package no.nav.k9.sak.kontrakt.vilkår;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.vilkår.VilkårType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VilkårMedPerioderDto {

    @JsonProperty(value = "vilkarType", required = true)
    @NotNull
    @Valid
    private VilkårType vilkarType;

    @JsonProperty(value = "lovReferanse")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{P}\\p{Space}\\p{L}\\p{Sc}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String lovReferanse;

    @JsonProperty(value = "overstyrbar")
    private Boolean overstyrbar;

    @JsonProperty(value = "perioder")
    @Valid
    private List<VilkårPeriodeDto> perioder;

    public VilkårMedPerioderDto() {
    }

    public VilkårMedPerioderDto(VilkårType vilkårType, List<VilkårPeriodeDto> perioder) {
        this.vilkarType = Objects.requireNonNull(vilkårType, "vilkårType");
        this.perioder = perioder;
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public String getLovReferanse() {
        return lovReferanse;
    }

    public void setLovReferanse(String lovReferanse) {
        this.lovReferanse = lovReferanse;
    }

    public List<VilkårPeriodeDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<VilkårPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    public boolean isOverstyrbar() {
        return overstyrbar;
    }

    public void setOverstyrbar(boolean overstyrbar) {
        this.overstyrbar = overstyrbar;
    }
}
