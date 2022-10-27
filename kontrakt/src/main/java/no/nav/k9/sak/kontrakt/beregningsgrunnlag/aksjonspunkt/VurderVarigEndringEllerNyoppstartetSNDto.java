package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderVarigEndringEllerNyoppstartetSNDto extends VurderVarigEndringEllerNyoppstartetDto implements VurderVarigEndring {

    @JsonProperty(value = "erVarigEndretNaering")
    private Boolean erVarigEndretNaering;

    public VurderVarigEndringEllerNyoppstartetSNDto() {
        //
    }

    public VurderVarigEndringEllerNyoppstartetSNDto(@Valid @NotNull Periode periode) {
        super(periode);
    }

    public boolean getErVarigEndretNaering() {
        return erVarigEndretNaering;
    }

    public void setErVarigEndretNaering(Boolean erVarigEndretNaering) {
        this.erVarigEndretNaering = erVarigEndretNaering;
    }

    @Override
    public Boolean erVarigEndret() {
        return erVarigEndretNaering || super.erVarigEndret;
    }

    @AssertTrue
    public boolean skalHaSattVurderVarigEndring() {
        return erVarigEndretNaering != null || erVarigEndret != null;
    }

}
