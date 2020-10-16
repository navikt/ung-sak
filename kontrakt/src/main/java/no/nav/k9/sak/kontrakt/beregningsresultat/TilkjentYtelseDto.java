package no.nav.k9.sak.kontrakt.beregningsresultat;

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
public class TilkjentYtelseDto {

    @JsonProperty(value = "perioder")
    @Size(max = 100)
    @Valid
    private List<TilkjentYtelsePeriodeDto> perioder;

    private TilkjentYtelseDto(Builder builder) {
        this.perioder = List.copyOf(builder.perioder);
    }

    protected TilkjentYtelseDto() {
        //
    }

    public static Builder build() {
        return new Builder();
    }

    public List<TilkjentYtelsePeriodeDto> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }


    public void setPerioder(List<TilkjentYtelsePeriodeDto> perioder) {
        this.perioder = List.copyOf(perioder);
    }

    public static class Builder {
        private List<TilkjentYtelsePeriodeDto> perioder;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public TilkjentYtelseDto create() {
            return new TilkjentYtelseDto(this);
        }

        public Builder medPerioder(List<TilkjentYtelsePeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }
    }
}
