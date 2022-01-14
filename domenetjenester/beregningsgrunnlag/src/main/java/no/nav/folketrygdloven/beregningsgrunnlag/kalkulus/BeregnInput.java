package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

public class BeregnInput {

    private final UUID bgReferanse;

    private final DatoIntervallEntitet vilkårsperiode;

    private final List<UUID> originalReferanser;

    private final InputOverstyringPeriode inputOverstyringPeriode;

    public BeregnInput(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode, List<UUID> originalReferanser, InputOverstyringPeriode inputOverstyringPeriode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
        this.originalReferanser = originalReferanser;
        this.inputOverstyringPeriode = inputOverstyringPeriode;
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

    public Optional<InputOverstyringPeriode> getInputOverstyringPeriode() {
        return Optional.ofNullable(inputOverstyringPeriode);
    }
    public List<UUID> getOriginalReferanser() {
        return originalReferanser == null ? Collections.emptyList() : originalReferanser;
    }
}
