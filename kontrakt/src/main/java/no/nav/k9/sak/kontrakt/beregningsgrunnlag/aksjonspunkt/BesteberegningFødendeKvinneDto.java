package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BesteberegningFødendeKvinneDto {

    @JsonProperty(value = "besteberegningAndelListe")
    @Valid
    @Size(max = 100)
    private List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe;

    @JsonProperty(value = "nyDagpengeAndel")
    @Valid
    private DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel;

    public BesteberegningFødendeKvinneDto() {
        //
    }

    public BesteberegningFødendeKvinneDto(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        this.besteberegningAndelListe = besteberegningAndelListe;
    }

    public BesteberegningFødendeKvinneDto(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe, DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        this.nyDagpengeAndel = nyDagpengeAndel;
        this.besteberegningAndelListe = besteberegningAndelListe;
    }

    public List<BesteberegningFødendeKvinneAndelDto> getBesteberegningAndelListe() {
        return besteberegningAndelListe;
    }

    public DagpengeAndelLagtTilBesteberegningDto getNyDagpengeAndel() {
        return nyDagpengeAndel;
    }

    public void setBesteberegningAndelListe(List<BesteberegningFødendeKvinneAndelDto> besteberegningAndelListe) {
        this.besteberegningAndelListe = besteberegningAndelListe;
    }
}
