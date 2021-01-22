package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.UUID;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class StartBeregningInput {

    private final UUID bgReferanse;

    private final DatoIntervallEntitet vilkårsperiode;

    public StartBeregningInput(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
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

}
