package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import no.nav.k9.kodeverk.uttak.PeriodeResultatType;

public class UttakDto {
    private final String periodeResultatType;
    private final boolean gradering;

    private UttakDto(String periodeResultatType, boolean gradering) {
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
