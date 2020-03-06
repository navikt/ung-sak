package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakTilsynsbehov {

    @JsonProperty(value = "prosent", required = true)
    @NotNull
    @Max(value = 200)
    private int prosent;

    public int getProsent() {
        return prosent;
    }

    public void setProsent(int prosent) {
        this.prosent = prosent;
    }

    @AssertTrue(message = "Tilsynsbehov må være 100 eller 200")
    private boolean isOk() {
        return prosent == 100 || prosent == 200;
    }
}
