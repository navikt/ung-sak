package no.nav.k9.sak.kontrakt.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

/**
 * Angir et samlet resultat av vilkår for perioder der ulike vilkår overlapper.
 * Typisk vil samlet resultat være {@link Utfall#IKKE_OPPFYLT} dersom et av vilkårene for en periode ikke er oppfylt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VilkårUtfallSamlet {

    @JsonProperty(value = "vilkårUtfall")
    @Valid
    private List<VilkårUtfall> vilkårUtfall;

    @JsonProperty(value = "samletUtfall", required = true)
    private Utfall samletUtfall;

    public static VilkårUtfallSamlet fra(List<VilkårUtfall> vilkårUtfall) {
        var samletUtfall = Utfall.ranger(vilkårUtfall.stream().map(VilkårUtfall::getVilkårStatus).collect(Collectors.toList()));
        return new VilkårUtfallSamlet(samletUtfall, vilkårUtfall);
    }

    public VilkårUtfallSamlet(Utfall samletUtfall, List<VilkårUtfall> vilkårUtfall) {
        this.vilkårUtfall = Collections.unmodifiableList(vilkårUtfall);
        this.samletUtfall = Objects.requireNonNull(samletUtfall);
    }

    public List<VilkårUtfall> getUnderliggendeVilkårUtfall() {
        return vilkårUtfall;
    }

    public Utfall getSamletUtfall() {
        return samletUtfall;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
    public static class VilkårUtfall {

        @JsonProperty(value = "vilkårType", required = true)
        @NotNull
        @Valid
        private VilkårType vilkårType;

        @JsonProperty(value = "avslagsårsak")
        @Valid
        private Avslagsårsak avslagsårsak;

        @JsonProperty(value = "vilkårUtfall", required = true)
        @NotNull
        @Valid
        private Utfall vilkårUtfall = Utfall.IKKE_VURDERT;

        @JsonCreator
        public VilkårUtfall(@JsonProperty(value = "vilkårType", required = true) VilkårType vilkårType,
                            @JsonProperty(value = "avslagsårsak") Avslagsårsak avslagsårsak,
                            @JsonProperty(value = "vilkårUtfall", required = true) Utfall vilkårUtfall) {
            this.vilkårType = vilkårType;
            this.avslagsårsak = avslagsårsak;
            this.vilkårUtfall = vilkårUtfall;
        }

        public VilkårType getVilkårType() {
            return vilkårType;
        }

        public Avslagsårsak getAvslagsårsak() {
            return avslagsårsak;
        }

        public void setAvslagsårsak(Avslagsårsak avslagsårsak) {
            this.avslagsårsak = avslagsårsak;
        }

        public Utfall getVilkårStatus() {
            return vilkårUtfall;
        }

        public void setUtfall(Utfall utfall) {
            this.vilkårUtfall = utfall;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof VilkårUtfall))
                return false;
            var other = (VilkårUtfall) obj;

            return Objects.equals(this.vilkårType, other.vilkårType)
                && Objects.equals(this.vilkårUtfall, other.vilkårUtfall)
                && Objects.equals(this.avslagsårsak, other.avslagsårsak);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vilkårType, vilkårUtfall, avslagsårsak);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                + "<vilkårType=" + vilkårType
                + (vilkårUtfall == null ? "" : ", utfall=" + vilkårUtfall)
                + (avslagsårsak == null ? "" : ", avslagsårsak=" + avslagsårsak) + ">";
        }
    }

}
