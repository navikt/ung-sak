package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public final class InfotrygdPårørendeSykdomRequest {
    private final String fødselsnummer;
    private final LocalDate fraOgMed;
    private final LocalDate tilOgMed;
    private final Set<String> relevanteBehandlingstemaer;

    private InfotrygdPårørendeSykdomRequest(String fødselsnummer, LocalDate fraOgMed, LocalDate tilOgMed, Set<String> relevanteBehandlingstemaer) {
        this.fødselsnummer = fødselsnummer;
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.relevanteBehandlingstemaer = relevanteBehandlingstemaer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return builder()
            .fødselsnummer(fødselsnummer)
            .fraOgMed(fraOgMed)
            .tilOgMed(tilOgMed)
            .relevanteBehandlingstemaer(relevanteBehandlingstemaer);
    }

    public String getFødselsnummer() {
        return fødselsnummer;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public Set<String> getRelevanteBehandlingstemaer() {
        return relevanteBehandlingstemaer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InfotrygdPårørendeSykdomRequest that = (InfotrygdPårørendeSykdomRequest) o;
        return Objects.equals(fødselsnummer, that.fødselsnummer) &&
            Objects.equals(fraOgMed, that.fraOgMed) &&
            Objects.equals(tilOgMed, that.tilOgMed) &&
            Objects.equals(relevanteBehandlingstemaer, that.relevanteBehandlingstemaer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fødselsnummer, fraOgMed, tilOgMed, relevanteBehandlingstemaer);
    }

    @Override
    public String toString() {
        return "InfotrygdPaaroerendeSykdomRequest<" +
            ", fødselsnummer=(maskert)" +
            ", fraOgMed=" + fraOgMed +
            ", tilOgMed=" + tilOgMed +
            ", relevanteBehandlingstemaer=" + relevanteBehandlingstemaer +
            '>';
    }

    public static final class Builder {
        private String fødselsnummer;
        private LocalDate fraOgMed;
        private LocalDate tilOgMed;
        private Set<String> relevanteBehandlingstemaer;

        public InfotrygdPårørendeSykdomRequest build() {
            return new InfotrygdPårørendeSykdomRequest(fødselsnummer, fraOgMed, tilOgMed, relevanteBehandlingstemaer);
        }

        public Builder fødselsnummer(String fødselsnummer) {
            this.fødselsnummer = fødselsnummer;
            return this;
        }

        public Builder fraOgMed(LocalDate fraOgMed) {
            this.fraOgMed = fraOgMed;
            return this;
        }

        public Builder tilOgMed(LocalDate tilOgMed) {
            this.tilOgMed = tilOgMed;
            return this;
        }

        public Builder relevanteBehandlingstemaer(Set<String> relevanteBehandlingstemaer) {
            this.relevanteBehandlingstemaer = relevanteBehandlingstemaer;
            return this;
        }
    }
}
