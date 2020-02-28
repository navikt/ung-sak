package no.nav.k9.sak.kontrakt.vilkår;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.typer.Periode;

/** Minimum vilkårresultat. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VilkårResultatDto {

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "avslagsårsak")
    @Valid
    private Avslagsårsak avslagsårsak;

    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private Utfall utfall = Utfall.IKKE_VURDERT;

    public VilkårResultatDto() {
        //
    }

    public VilkårResultatDto(@NotNull @Valid Periode periode, @Valid Avslagsårsak avslagsårsak, @NotNull @Valid Utfall utfall) {
        this.periode = periode;
        this.avslagsårsak = avslagsårsak;
        this.utfall = utfall;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }

    public void setAvslagsårsak(Avslagsårsak avslagsårsak) {
        this.avslagsårsak = avslagsårsak;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public void setUtfall(Utfall utfall) {
        this.utfall = utfall;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof VilkårResultatDto))
            return false;
        var other = (VilkårResultatDto) obj;

        return Objects.equals(this.periode, other.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.periode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + periode
            + (utfall == null ? "" : ", utfall=" + utfall)
            + (avslagsårsak == null ? "" : ", avslagsårsak=" + avslagsårsak) + ">";
    }
}
