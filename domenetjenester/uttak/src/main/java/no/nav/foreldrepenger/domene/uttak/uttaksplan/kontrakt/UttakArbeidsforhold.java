package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakArbeidsforhold {

    @JsonValue
    @NotNull
    private Map<Periode, UttakArbeidsforholdInfo> perioder;

    public Map<Periode, UttakArbeidsforholdInfo> getPerioder() {
        return perioder;
    }

    public void setPerioder(Map<Periode, UttakArbeidsforholdInfo> perioder) {
        this.perioder = perioder;
    }
    
}
