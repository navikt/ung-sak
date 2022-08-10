package no.nav.k9.sak.kontrakt.omsorgspenger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ALDERSVILKÅR_BARN)
public class AvklarAldersvilkårBarnDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "erVilkarOk", required = true)
    @NotNull
    private Boolean erVilkarOk;

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    /** Angitt avslagsårsak (dersom erVilkarOk==false) */
    @JsonProperty(value = "avslagsårsak")
    @Valid
    private Avslagsårsak avslagsårsak;

    public AvklarAldersvilkårBarnDto() {
        //
    }

    public AvklarAldersvilkårBarnDto(String begrunnelse,
                                     Boolean erVilkarOk,
                                     Periode periode,
                                     Avslagsårsak avslagsårsak) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.periode = periode;
        this.avslagsårsak = avslagsårsak;
    }

    @AssertTrue(message = "Kan ikke angi avslagsårsak dersom vilkår er ok")
    private boolean isOk() {
        return !erVilkarOk || (erVilkarOk && avslagsårsak == null);
    }

    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
