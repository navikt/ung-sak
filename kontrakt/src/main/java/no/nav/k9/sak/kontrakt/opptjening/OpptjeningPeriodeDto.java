package no.nav.k9.sak.kontrakt.opptjening;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OpptjeningPeriodeDto {

    @JsonProperty(value = "dager")
    @Min(value = 0)
    @Max(value = 31)
    private int dager;

    @JsonProperty(value = "måneder")
    @Min(value = 0)
    @Max(value = 12 * 3)
    private int måneder;

    public OpptjeningPeriodeDto() {
        // trengs for deserialisering av JSON
        this.måneder = 0;
        this.dager = 0;
    }

    public OpptjeningPeriodeDto(int måneder, int dager) {
        this.måneder = måneder;
        this.dager = dager;
    }

    public int getDager() {
        return dager;
    }

    public int getMåneder() {
        return måneder;
    }

    public void setDager(int dager) {
        this.dager = dager;
    }

    public void setMåneder(int måneder) {
        this.måneder = måneder;
    }
}
