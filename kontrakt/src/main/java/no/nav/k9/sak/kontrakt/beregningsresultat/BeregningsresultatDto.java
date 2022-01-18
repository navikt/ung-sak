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
public class BeregningsresultatDto {

    public static class Builder {
        private LocalDate opphoersdato;
        private List<BeregningsresultatPeriodeDto> perioder;
        private Boolean skalHindreTilbaketrekk;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public BeregningsresultatDto create() {
            return new BeregningsresultatDto(this);
        }

        public Builder medOpphoersdato(LocalDate opphoersdato) {
            this.opphoersdato = opphoersdato;
            return this;
        }

        public Builder medPerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.perioder = perioder;
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

    @JsonProperty(value = "skalHindreTilbaketrekk")
    private Boolean skalHindreTilbaketrekk;

    private BeregningsresultatDto(Builder builder) {
        this.opphoersdato = builder.opphoersdato;
        this.perioder = List.copyOf(builder.perioder);
        this.skalHindreTilbaketrekk = builder.skalHindreTilbaketrekk;
    }

    protected BeregningsresultatDto() {
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

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }

    public void setOpphoersdato(LocalDate opphoersdato) {
        this.opphoersdato = opphoersdato;
    }

    public void setPerioder(List<BeregningsresultatPeriodeDto> perioder) {
        this.perioder = List.copyOf(perioder);
    }

    public void setSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
        this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
    }
}
