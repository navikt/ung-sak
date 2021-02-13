package no.nav.k9.sak.behandling;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Inneholder relevante skjæringstidspunkter for en behandling
 */
public class Skjæringstidspunkt {
    private LocalDate utledetSkjæringstidspunkt;

    private Skjæringstidspunkt() {
        // hide constructor
    }

    private Skjæringstidspunkt(Skjæringstidspunkt other) {
        this.utledetSkjæringstidspunkt = other.utledetSkjæringstidspunkt;
    }

    public Optional<LocalDate> getSkjæringstidspunktHvisUtledet() {
        return Optional.ofNullable(utledetSkjæringstidspunkt);
    }

    public LocalDate getUtledetSkjæringstidspunkt() {
        Objects.requireNonNull(utledetSkjæringstidspunkt, "Utvikler-feil: utledetSkjæringstidspunkt er ikke satt. Sørg for at det er satt ifht. anvendelse");
        return utledetSkjæringstidspunkt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utledetSkjæringstidspunkt);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj.getClass().equals(this.getClass()))) {
            return false;
        }
        Skjæringstidspunkt other = (Skjæringstidspunkt) obj;
        return Objects.equals(this.utledetSkjæringstidspunkt, other.utledetSkjæringstidspunkt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + utledetSkjæringstidspunkt + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Skjæringstidspunkt other) {
        return new Builder(other);
    }

    public static class Builder {
        private final Skjæringstidspunkt kladd;

        private Builder() {
            this.kladd = new Skjæringstidspunkt();
        }

        private Builder(Skjæringstidspunkt other) {
            this.kladd = new Skjæringstidspunkt(other);
        }

        public Builder medUtledetSkjæringstidspunkt(LocalDate dato) {
            kladd.utledetSkjæringstidspunkt = dato;
            return this;
        }

        public Skjæringstidspunkt build() {
            return kladd;
        }
    }
}
