package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.util.Optional;
import java.util.UUID;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

public class KalkulusBeregningsgrunnlagGrunnlag extends BeregningsgrunnlagGrunnlag {

    @DiffIgnore
    private UUID koblingReferanse;

    public KalkulusBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlag grunnlag, String koblingReferanse) {
        this(grunnlag, UUID.fromString(koblingReferanse));
    }

    public KalkulusBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlag grunnlag, UUID koblingReferanse) {
        super(grunnlag);
        this.koblingReferanse = koblingReferanse;
    }

    @Override
    public Optional<UUID> getKoblingReferanse() {
        return Optional.of(this.koblingReferanse);
    }
}
