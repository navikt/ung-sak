package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeregningsresultatDto {
    private LocalDate opphoersdato;
    private final BeregningsresultatPeriodeDto[] perioder;
    private final Boolean skalHindreTilbaketrekk;

    private BeregningsresultatDto(Builder builder) {
        this.opphoersdato = builder.opphoersdato;
        this.perioder = builder.perioder.stream().toArray(BeregningsresultatPeriodeDto[]::new);
        this.skalHindreTilbaketrekk = builder.skalHindreTilbaketrekk;
    }

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    public BeregningsresultatPeriodeDto[] getPerioder() {
        return Arrays.copyOf(perioder, perioder.length);
    }

    public Boolean getSkalHindreTilbaketrekk() {
        return skalHindreTilbaketrekk;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate opphoersdato;
        private List<BeregningsresultatPeriodeDto> perioder;
        private Boolean skalHindreTilbaketrekk;

        private Builder() {
            perioder = new ArrayList<>();
        }

        public Builder medOpphoersdato(LocalDate opphoersdato) {
            this.opphoersdato = opphoersdato;
            return this;
        }

        public Builder medSkalHindreTilbaketrekk(Boolean skalHindreTilbaketrekk) {
            this.skalHindreTilbaketrekk = skalHindreTilbaketrekk;
            return this;
        }

        public Builder medPerioder(List<BeregningsresultatPeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }

        public BeregningsresultatDto create() {
            return new BeregningsresultatDto(this);
        }
    }
}
