package no.nav.k9.sak.kontrakt.omsorgspenger;

import javax.validation.Valid;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE)
public class AvklarOmsorgenForDto extends BekreftetAksjonspunktDto {
    
    @JsonProperty(value = "harOmsorgenFor", required = true)
    @NotNull
    private Boolean harOmsorgenFor;

    @Valid
    private Periode periode;

    /** (optional) Angitt avslagsårsak (dersom erVilkarOk==false) */
    @JsonProperty(value = "avslagsårsak")
    @Valid
    private Avslagsårsak avslagsårsak;

    public AvklarOmsorgenForDto(String begrunnelse, Boolean harOmsorgenFor, Periode periode, Avslagsårsak avslagsårsak) {
        super(begrunnelse);
        this.harOmsorgenFor = harOmsorgenFor;
        this.periode = periode;
        this.avslagsårsak = avslagsårsak;
    }

    protected AvklarOmsorgenForDto() {
        //
    }

    public Boolean getHarOmsorgenFor() {
        return harOmsorgenFor;
    }

    public Boolean getErVilkarOk() {
        return getHarOmsorgenFor();
    }

    public Periode getPeriode() {
        return periode;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
