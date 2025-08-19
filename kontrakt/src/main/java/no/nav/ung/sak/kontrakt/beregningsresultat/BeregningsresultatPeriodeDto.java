package no.nav.ung.sak.kontrakt.beregningsresultat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatPeriodeDto {

    public static class Builder {
        private int dagsats;
        private LocalDate fom;
        private LocalDate tom;

        private Builder() {
        }

        public BeregningsresultatPeriodeDto create() {
            return new BeregningsresultatPeriodeDto(this);
        }


        public Builder medDagsats(int dagsats) {
            this.dagsats = dagsats;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            this.tom = tom;
            return this;
        }
    }

    @JsonProperty(value = "dagsats", required = true)
    @Min(0)
    @Max(100000)
    private int dagsats;

    @JsonProperty(value = "fom", required = true)
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @Valid
    private LocalDate tom;

    private BeregningsresultatPeriodeDto(Builder builder) {
        fom = builder.fom;
        tom = builder.tom;
        dagsats = builder.dagsats;
    }

    public BeregningsresultatPeriodeDto() {
        // Deserialisering av JSON
    }

    public static Builder build() {
        return new Builder();
    }

    public static Builder build(LocalDate fom, LocalDate tom) {
        return new Builder().medFom(fom).medTom(tom);
    }


    public int getDagsats() {
        return dagsats;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }


    public void setDagsats(int dagsats) {
        this.dagsats = dagsats;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
