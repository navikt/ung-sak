package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilkjentYtelsePeriodeDto {

    public static class Builder {
        private List<TilkjentYtelseAndelDto> andeler;
        private LocalDate fom;
        private LocalDate tom;

        private Builder() {
            this.andeler = new ArrayList<>();
        }

        public TilkjentYtelsePeriodeDto create() {
            return new TilkjentYtelsePeriodeDto(this);
        }

        public Builder medAndeler(List<TilkjentYtelseAndelDto> andeler) {
            this.andeler.addAll(andeler);
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
    private List<TilkjentYtelseAndelDto> andeler;

    @JsonProperty(value = "fom", required = true)
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @Valid
    private LocalDate tom;

    private TilkjentYtelsePeriodeDto(Builder builder) {
        fom = builder.fom;
        tom = builder.tom;
        andeler = List.copyOf(builder.andeler);
    }

    public TilkjentYtelsePeriodeDto() {
        // Deserialisering av JSON
    }

    public static Builder build() {
        return new Builder();
    }

    public static Builder build(LocalDate fom, LocalDate tom) {
        return new Builder().medFom(fom).medTom(tom);
    }

    public List<TilkjentYtelseAndelDto> getAndeler() {
        return Collections.unmodifiableList(andeler);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setAndeler(List<TilkjentYtelseAndelDto> andeler) {
        this.andeler = List.copyOf(andeler);
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
