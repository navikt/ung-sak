package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttaksperiodeInfo {

    @JsonProperty(value="type", required=true)
    @NotNull
    @Valid
    private UttaksperiodeType type;
    
    @JsonProperty(value="grad")
    @Min(0)
    @Max(100)
    private int grad;
    
    @JsonProperty(value="utbetalingsgrader")
    @Valid
    private Map<UUID, UttakUtbetalingsgrad> utbetalingsgrader = new LinkedHashMap<>();
    
    public UttaksperiodeType getType() {
        return type;
    }

    public void setType(UttaksperiodeType type) {
        this.type = type;
    }

    public int getGrad() {
        return grad;
    }

    public void setGrad(int grad) {
        this.grad = grad;
    }

    public Map<UUID, UttakUtbetalingsgrad> getUtbetalingsgrader() {
        return utbetalingsgrader;
    }

    public void setUtbetalingsgrader(Map<UUID, UttakUtbetalingsgrad> utbetalingsgrader) {
        this.utbetalingsgrader = utbetalingsgrader;
    }

    public enum UttaksperiodeType {
        INNVILGET,
        AVSLÅTT;
    }
    
}
