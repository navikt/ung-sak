package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatPeriodeDto {

    public static class Builder {
        private List<BeregningsresultatPeriodeAndelDto> andeler;
        private int dagsats;
        private LocalDate fom;
        private LocalDate tom;

        private Builder() {
            this.andeler = new ArrayList<>();
        }

        public BeregningsresultatPeriodeDto create() {
            return new BeregningsresultatPeriodeDto(this);
        }

        public Builder medAndeler(List<BeregningsresultatPeriodeAndelDto> andeler) {
            this.andeler.addAll(andeler);
            return this;
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

    @JsonProperty(value = "andeler", required = true)
    @Valid
    @Size(max = 200)
    private List<BeregningsresultatPeriodeAndelDto> andeler;

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
        andeler = List.copyOf(builder.andeler);
    }

    public BeregningsresultatPeriodeDto() {
        // Deserialisering av JSON
    }

    public static Builder build() {
        return new Builder();
    }

    public List<BeregningsresultatPeriodeAndelDto> getAndeler() {
        return Collections.unmodifiableList(andeler);
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

    public void setAndeler(List<BeregningsresultatPeriodeAndelDto> andeler) {
        this.andeler = List.copyOf(andeler);
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
