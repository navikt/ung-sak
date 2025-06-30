package no.nav.ung.sak.kontrakt.beregningsresultat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatDto {

    public static class Builder {
        private List<BeregningsresultatPeriodeDto> perioder;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public BeregningsresultatDto create() {
            return new BeregningsresultatDto(this);
        }


        public Builder medPerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }

    }


    @JsonProperty(value = "perioder")
    @Size(max = 100)
    @Valid
    private List<BeregningsresultatPeriodeDto> perioder;

    private BeregningsresultatDto(Builder builder) {
        this.perioder = List.copyOf(builder.perioder);
    }

    protected BeregningsresultatDto() {
        //
    }

    public static Builder build() {
        return new Builder();
    }

    public List<BeregningsresultatPeriodeDto> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public void setPerioder(List<BeregningsresultatPeriodeDto> perioder) {
        this.perioder = List.copyOf(perioder);
    }

}
