package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderATogFLiSammeOrganisasjonDto {

    @JsonProperty(value = "vurderATogFLiSammeOrganisasjonAndelListe", required = true)
    @Valid
    @NotNull
    @Size(max = 100)
    private List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe;

    protected VurderATogFLiSammeOrganisasjonDto() {
        //
    }

    public VurderATogFLiSammeOrganisasjonDto(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        this.vurderATogFLiSammeOrganisasjonAndelListe = vurderATogFLiSammeOrganisasjonAndelListe;
    }

    public List<VurderATogFLiSammeOrganisasjonAndelDto> getVurderATogFLiSammeOrganisasjonAndelListe() {
        return vurderATogFLiSammeOrganisasjonAndelListe;
    }

    public void setVurderATogFLiSammeOrganisasjonAndelListe(List<VurderATogFLiSammeOrganisasjonAndelDto> vurderATogFLiSammeOrganisasjonAndelListe) {
        this.vurderATogFLiSammeOrganisasjonAndelListe = vurderATogFLiSammeOrganisasjonAndelListe;
    }
}
