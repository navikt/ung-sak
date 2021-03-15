package no.nav.k9.sak.kontrakt.omsorgspenger;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OMS_UTVIDET_RETT)
public class AvklarUtvidetRettDto extends BekreftetAksjonspunktDto {

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

    public AvklarUtvidetRettDto() {
        //
    }

    public AvklarUtvidetRettDto(String begrunnelse,
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
