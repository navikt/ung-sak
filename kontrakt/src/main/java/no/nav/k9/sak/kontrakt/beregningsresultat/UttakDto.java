package no.nav.k9.sak.kontrakt.beregningsresultat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.PeriodeResultatType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakDto {

    public static class Builder {
        private boolean gradering;
        private PeriodeResultatType periodeResultatType;

        private Builder() {
        }

        public UttakDto create() {
            String periodeResultatTypeString = periodeResultatType == null ? null : periodeResultatType.getKode();
            return new UttakDto(periodeResultatTypeString, gradering);
        }

        public Builder medGradering(boolean gradering) {
            this.gradering = gradering;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            this.periodeResultatType = periodeResultatType;
            return this;
        }
    }

    @JsonProperty(value = "gradering", required = true)
    @NotNull
    private boolean gradering;

    @JsonProperty(value = "periodeResultatType", required = true)
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}_\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String periodeResultatType;

    public UttakDto(String periodeResultatType, boolean gradering) {
        this.periodeResultatType = periodeResultatType;
        this.gradering = gradering;
    }

    protected UttakDto() {
        //
    }

    public static Builder build() {
        return new Builder();
    }

    public String getPeriodeResultatType() {
        return periodeResultatType;
    }

    public boolean isGradering() {
        return gradering;
    }
}
