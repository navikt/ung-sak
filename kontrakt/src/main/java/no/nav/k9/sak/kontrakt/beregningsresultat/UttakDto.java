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

    @JsonProperty(value = "periodeResultatType", required = true)
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}_\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String periodeResultatType;

    @JsonProperty(value = "gradering", required = true)
    @NotNull
    private boolean gradering;

    protected UttakDto() {
        //
    }

    public UttakDto(String periodeResultatType, boolean gradering) {
        this.periodeResultatType = periodeResultatType;
        this.gradering = gradering;
    }

    public String getPeriodeResultatType() {
        return periodeResultatType;
    }

    public boolean isGradering() {
        return gradering;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private PeriodeResultatType periodeResultatType;
        private boolean gradering;

        private Builder() {
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            this.periodeResultatType = periodeResultatType;
            return this;
        }

        public Builder medGradering(boolean gradering) {
            this.gradering = gradering;
            return this;
        }

        public UttakDto create() {
            String periodeResultatTypeString = periodeResultatType == null ? null : periodeResultatType.getKode();
            return new UttakDto(periodeResultatTypeString, gradering);
        }
    }
}
