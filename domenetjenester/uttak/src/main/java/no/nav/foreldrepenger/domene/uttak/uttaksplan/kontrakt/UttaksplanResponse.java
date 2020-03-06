package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttaksplanResponse {

    @JsonProperty(value = "perioder", required = true)
    @Valid
    private Map<Periode, UttaksperiodeInfo> perioder = new LinkedHashMap<>();

    public Map<Periode, UttaksperiodeInfo> getPerioder() {
        return perioder;
    }

    public void setPerioder(Map<Periode, UttaksperiodeInfo> perioder) {
        this.perioder = perioder;
    }
}
