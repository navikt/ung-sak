package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

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
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE)
public class OverstyrBeregningsaktiviteterDto extends OverstyringAksjonspunktDto {

    @JsonProperty(value = "beregningsaktivitetLagreDtoList")
    @Valid
    @Size(max = 1000)
    private List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList;

    public OverstyrBeregningsaktiviteterDto() {
        //
    }

    public OverstyrBeregningsaktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList, String begrunnelse, @Valid @NotNull Periode periode) { // NOSONAR
        super(periode, begrunnelse);
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    public List<BeregningsaktivitetLagreDto> getBeregningsaktivitetLagreDtoList() {
        return beregningsaktivitetLagreDtoList;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }

    public void setBeregningsaktivitetLagreDtoList(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
    }
}
