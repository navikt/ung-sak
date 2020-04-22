package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatMedUtbetaltePeriodeDto {

    public static class Builder {
        private LocalDate opphoersdato;
        private List<BeregningsresultatPeriodeDto> perioder;
        private List<BeregningsresultatPeriodeDto> utbetaltePerioder;
        private Boolean skalHindreTilbaketrekk;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public BeregningsresultatMedUtbetaltePeriodeDto create() {
            return new BeregningsresultatMedUtbetaltePeriodeDto(this);
        }

        public Builder medOpphoersdato(LocalDate opphoersdato) {
            this.opphoersdato = opphoersdato;
            return this;
        }

        public Builder medPerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }

        public Builder medUtbetaltePerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.utbetaltePerioder = perioder;
            return this;
        }

        public Builder medSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
            this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
            return this;
        }
    }

    @JsonProperty(value = "opphoersdato")
    @Valid
    private LocalDate opphoersdato;

    @JsonProperty(value = "perioder")
    @Size(max = 100)
    @Valid
    private List<BeregningsresultatPeriodeDto> perioder;

    @JsonProperty(value = "utbetaltePerioder")
    @Size(max = 100)
    @Valid
    private List<BeregningsresultatPeriodeDto> utbetaltePerioder;

    @JsonProperty(value = "skalHindreTilbaketrekk")
    private Boolean skalHindreTilbaketrekk;

    @JsonCreator
    public BeregningsresultatMedUtbetaltePeriodeDto(@JsonProperty(value = "opphoersdato") @Valid LocalDate opphoersdato,
                                                    @JsonProperty(value = "perioder") @Size(max = 100) @Valid List<BeregningsresultatPeriodeDto> perioder,
                                                    @JsonProperty(value = "utbetaltePerioder") @Size(max = 100) @Valid List<BeregningsresultatPeriodeDto> utbetaltePerioder,
                                                    @JsonProperty(value = "skalHindreTilbaketrekk") Boolean skalHindreTilbaketrekk) {
        this.opphoersdato = opphoersdato;
        this.perioder = perioder;
        this.utbetaltePerioder = utbetaltePerioder;
        this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
    }

    private BeregningsresultatMedUtbetaltePeriodeDto(Builder builder) {
        this.opphoersdato = builder.opphoersdato;
        this.perioder = List.copyOf(builder.perioder);
        this.utbetaltePerioder = List.copyOf(builder.utbetaltePerioder);
        this.skalHindreTilbaketrekk = builder.skalHindreTilbaketrekk;
    }

    protected BeregningsresultatMedUtbetaltePeriodeDto() {
        //
    }

    public static Builder build() {
        return new Builder();
    }

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    public List<BeregningsresultatPeriodeDto> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }

    public List<BeregningsresultatPeriodeDto> getUtbetaltePerioder() {
        return Collections.unmodifiableList(utbetaltePerioder);
    }

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }

}
