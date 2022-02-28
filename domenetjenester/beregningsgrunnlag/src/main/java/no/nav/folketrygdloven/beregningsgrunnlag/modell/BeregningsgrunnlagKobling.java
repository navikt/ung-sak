package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class BeregningsgrunnlagKobling {

    private LocalDate skjæringstidspunkt;
    private UUID referanse;
    private boolean erTilVurdering;

    public BeregningsgrunnlagKobling(LocalDate skjæringstidspunkt, UUID referanse, boolean erTilVurdering) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.referanse = referanse;
        this.erTilVurdering = erTilVurdering;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public boolean getErTilVurdering() {
        return erTilVurdering;
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
            ", erTilVurdering=" + erTilVurdering +
            '}';
    }
}
