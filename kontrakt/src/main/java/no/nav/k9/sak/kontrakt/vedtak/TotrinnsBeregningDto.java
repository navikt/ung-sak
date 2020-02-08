package no.nav.k9.sak.kontrakt.vedtak;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnsBeregningDto {

    @JsonProperty(value = "fastsattVarigEndringNaering")
    private boolean fastsattVarigEndringNaering;

    @JsonProperty(value = "faktaOmBeregningTilfeller")
    @Size(max = 100)
    @Valid
    private List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller;

    public void setFastsattVarigEndringNaering(boolean fastsattVarigEndringNaering) {
        this.fastsattVarigEndringNaering = fastsattVarigEndringNaering;
    }

    public boolean isFastsattVarigEndringNaering() {
        return fastsattVarigEndringNaering;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return faktaOmBeregningTilfeller;
    }

    public void setFaktaOmBeregningTilfeller(List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        this.faktaOmBeregningTilfeller = faktaOmBeregningTilfeller;
    }
}
