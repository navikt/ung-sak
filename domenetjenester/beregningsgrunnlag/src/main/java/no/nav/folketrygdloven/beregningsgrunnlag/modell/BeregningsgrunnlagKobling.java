package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class BeregningsgrunnlagKobling {

    private LocalDate skjæringstidspunkt;
    private UUID referanse;
    private boolean erForlengelse;

    public BeregningsgrunnlagKobling(LocalDate skjæringstidspunkt, UUID referanse, boolean erForlengelse) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.referanse = referanse;
        this.erForlengelse = erForlengelse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean getErForlengelse() {
        return erForlengelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeregningsgrunnlagKobling that = (BeregningsgrunnlagKobling) o;
        return Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt) &&
            Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, referanse);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagKobling{" +
            "skjæringstidspunkt=" + skjæringstidspunkt +
            ", referanse=" + referanse +
            ", erForlengelse=" + erForlengelse +
            '}';
    }
}
