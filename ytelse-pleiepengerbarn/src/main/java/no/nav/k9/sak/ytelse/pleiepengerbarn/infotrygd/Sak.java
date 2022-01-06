package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = Sak.Builder.class)
class Sak {
    private final Kodeverdi tema;
    private final Kodeverdi behandlingstema;
    private final Kodeverdi resultat;
    private final LocalDate opphoerFom;

    private Sak(Kodeverdi tema, Kodeverdi behandlingstema, Kodeverdi resultat, LocalDate opphoerFom) {
        this.tema = tema;
        this.behandlingstema = behandlingstema;
        this.resultat = resultat;
        this.opphoerFom = opphoerFom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Kodeverdi getTema() {
        return tema;
    }

    public Kodeverdi getResultat() {
        return resultat;
    }

    public LocalDate getOpphoerFom() {
        return opphoerFom;
    }

    public Kodeverdi getBehandlingstema() {
        return behandlingstema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sak sak = (Sak) o;
        return Objects.equals(tema, sak.tema) &&
                Objects.equals(behandlingstema, sak.behandlingstema) &&
                Objects.equals(resultat, sak.resultat) &&
                Objects.equals(opphoerFom, sak.opphoerFom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tema, behandlingstema, resultat, opphoerFom);
    }

    @Override
    public String toString() {
        return "Sak<" +
                "tema=" + tema +
                ", behandlingstema=" + behandlingstema +
                ", resultat=" + resultat +
                ", opphoerFom=" + opphoerFom +
                '>';
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private Kodeverdi tema;
        private Kodeverdi behandlingstema;
        private Kodeverdi resultat;
        private LocalDate opphoerFom;

        public Sak build() {
            return new Sak(tema, behandlingstema, resultat, opphoerFom);
        }

        public Builder tema(Kodeverdi tema) {
            this.tema = tema;
            return this;
        }

        public Builder behandlingstema(Kodeverdi behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public Builder resultat(Kodeverdi resultat) {
            this.resultat = resultat;
            return this;
        }

        public Builder opphoerFom(LocalDate opphoerFom) {
            this.opphoerFom = opphoerFom;
            return this;
        }
    }
}
