package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE)
public class OverstyrBeregningsgrunnlagDto extends OverstyringAksjonspunktDto {


    @JsonProperty(value = "overstyrteAndeler")
    @Valid
    @Size(max = 100)
    private List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler;

    public OverstyrBeregningsgrunnlagDto() {
        //
    }

    public OverstyrBeregningsgrunnlagDto(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler, String begrunnelse, @Valid @NotNull Periode periode) { // NOSONAR
        super(periode, begrunnelse);
        this.overstyrteAndeler = overstyrteAndeler;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }

    public List<FastsettBeregningsgrunnlagAndelDto> getOverstyrteAndeler() {
        return overstyrteAndeler;
    }

    public void setOverstyrteAndeler(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler) {
        this.overstyrteAndeler = overstyrteAndeler;
    }

    @AssertTrue
    public boolean skalVereAvbruttEllerHaVerdier() {
        return skalAvbrytes() || overstyrteAndeler != null;
    }

}
