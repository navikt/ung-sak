package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakTilsynsbehov {

    @JsonProperty(value = "prosent", required = true)
    @NotNull
    @Min(value = 0)
    @Max(value = 200)
    private int prosent;

    @JsonCreator
    public UttakTilsynsbehov(@JsonProperty(value = "prosent", required = true) @NotNull @Max(value = 200) int prosent) {
        if (prosent < 0 || prosent > 200) {
            throw new IllegalArgumentException("prosent " + prosent + "kan ikke være < 0 eller > 200");
        }
        this.prosent = prosent;
    }

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
