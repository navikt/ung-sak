package no.nav.k9.sak.kontrakt.behandling;

import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.vilkår.VilkårResultatDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingsresultatDto {

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "erRevurderingMedUendretUtfall")
    @Valid
    private Boolean erRevurderingMedUendretUtfall;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private SkjæringstidspunktDto skjæringstidspunkt;

    @JsonProperty(value = "type", required = true)
    @NotNull
    @Valid
    private BehandlingResultatType type = BehandlingResultatType.IKKE_FASTSATT;

    @JsonProperty(value="vilkårResultat")
    private Map<VilkårType, Set<VilkårResultatDto>> vilkårResultat;

    public BehandlingsresultatDto() {
        //
    }

    public Boolean getErRevurderingMedUendretUtfall() {
        return Boolean.TRUE.equals(erRevurderingMedUendretUtfall);
    }

    public SkjæringstidspunktDto getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public BehandlingResultatType getResultatType() {
        return type;
    }


    public Map<VilkårType, Set<VilkårResultatDto>> getVilkårResultat() {
        return vilkårResultat;
    }

    public void setErRevurderingMedUendretUtfall(Boolean erRevurderingMedUendretUtfall) {
        this.erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall;
    }

    public void setSkjæringstidspunkt(SkjæringstidspunktDto skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setResultatType(BehandlingResultatType type) {
        this.type = type;
    }

    public void setVilkårResultat(Map<VilkårType, Set<VilkårResultatDto>> vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }
}
