package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class BeregningsgrunnlagReferanse {
    private final UUID referanse;
    private final LocalDate skjæringstidspunkt;

    public BeregningsgrunnlagReferanse(UUID referanse, LocalDate skjæringstidspunkt) {
        this.referanse = Objects.requireNonNull(referanse);
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
    }

    public UUID getReferanse() {
        return referanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }
}
