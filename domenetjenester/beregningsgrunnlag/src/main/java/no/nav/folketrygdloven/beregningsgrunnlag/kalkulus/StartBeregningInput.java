package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class StartBeregningInput {

    private final UUID bgReferanse;

    private final DatoIntervallEntitet vilkårsperiode;

    private final List<UUID> originalReferanser;

    public StartBeregningInput(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode, List<UUID> originalReferanser) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
        this.originalReferanser = originalReferanser;
    }

    public UUID getBgReferanse() {
        return bgReferanse;
    }

    public DatoIntervallEntitet getVilkårsperiode() {
        return vilkårsperiode;
    }

    public LocalDate getSkjæringstidspunkt() {
        return vilkårsperiode.getFomDato();
    }

    public List<UUID> getOriginalReferanser() {
        return originalReferanser == null ? Collections.emptyList() : originalReferanser;
    }
}
