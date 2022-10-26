package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderVarigEndretArbeidssituasjonDto extends VurderVarigEndringEllerNyoppstartetDto implements VurderVarigEndring {

    public VurderVarigEndretArbeidssituasjonDto() {
        //
    }

    public VurderVarigEndretArbeidssituasjonDto(@Valid @NotNull Periode periode) {
        super(periode);
    }

    @Override
    public Boolean erVarigEndret() {
        return erVarigEndret;
    }

    @AssertTrue
    public boolean skalHaSattVurderVarigEndring() {
        return erVarigEndret != null;
    }
}
