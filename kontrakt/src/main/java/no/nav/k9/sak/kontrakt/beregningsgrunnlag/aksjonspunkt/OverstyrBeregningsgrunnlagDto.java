package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE)
public class OverstyrBeregningsgrunnlagDto extends OverstyringAksjonspunktDto {

    @JsonProperty(value = "endringer")
    @Size(max = 200)
    @Valid
    private Set<Lønnsendring> endringer;

    @JsonProperty(value = "fakta")
    @Valid
    private FaktaBeregningLagreDto fakta;

    @JsonProperty(value = "overstyrteAndeler", required = true)
    @Valid
    @NotNull
    @Size(max = 100)
    private List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler;

    public OverstyrBeregningsgrunnlagDto() {
        //
    }

    public OverstyrBeregningsgrunnlagDto(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.overstyrteAndeler = overstyrteAndeler;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    public Set<Lønnsendring> getEndringer() {
        return endringer;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }

    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }

    public List<FastsettBeregningsgrunnlagAndelDto> getOverstyrteAndeler() {
        return overstyrteAndeler;
    }

    public void setEndringer(Set<Lønnsendring> endringer) {
        this.endringer = endringer;
    }

    public void setFakta(FaktaBeregningLagreDto fakta) {
        this.fakta = fakta;
    }

    public void setOverstyrteAndeler(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler) {
        this.overstyrteAndeler = overstyrteAndeler;
    }
}
