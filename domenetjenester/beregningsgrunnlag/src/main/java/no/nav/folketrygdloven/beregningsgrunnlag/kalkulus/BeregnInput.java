package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

public class BeregnInput {

    private final UUID bgReferanse;

    private final DatoIntervallEntitet vilkårsperiode;

    private boolean erForlengelse;

    private Map<LocalDate, UUID> originalReferanser;

    private InputOverstyringPeriode inputOverstyringPeriode;

    public BeregnInput(UUID bgReferanse, PeriodeTilVurdering vilkårsperiode, Map<LocalDate, UUID> originalReferanser, InputOverstyringPeriode inputOverstyringPeriode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode.getPeriode();
        this.erForlengelse = vilkårsperiode.erForlengelse();
        this.originalReferanser = originalReferanser;
        this.inputOverstyringPeriode = inputOverstyringPeriode;
    }

    public BeregnInput(UUID bgReferanse,
                       DatoIntervallEntitet vilkårsperiode,
                       boolean erForlengelse,
                       Map<LocalDate, UUID> originalReferanser,
                       InputOverstyringPeriode inputOverstyringPeriode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
        this.erForlengelse = erForlengelse;
        this.originalReferanser = originalReferanser;
        this.inputOverstyringPeriode = inputOverstyringPeriode;
    }


    public BeregnInput(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode, Map<LocalDate, UUID> originalReferanser, InputOverstyringPeriode inputOverstyringPeriode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
        this.originalReferanser = originalReferanser;
        this.inputOverstyringPeriode = inputOverstyringPeriode;
    }

    private BeregnInput(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode) {
        this.bgReferanse = bgReferanse;
        this.vilkårsperiode = vilkårsperiode;
    }

    public static BeregnInput forAksjonspunktOppdatering(UUID bgReferanse, DatoIntervallEntitet vilkårsperiode) {
        return new BeregnInput(bgReferanse, vilkårsperiode);
    }

    public UUID getBgReferanse() {
        return bgReferanse;
    }

    public DatoIntervallEntitet getVilkårsperiode() {
        return vilkårsperiode;
    }

    public boolean erForlengelse() {
        return erForlengelse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return vilkårsperiode.getFomDato();
    }

    public Optional<InputOverstyringPeriode> getInputOverstyringPeriode() {
        return Optional.ofNullable(inputOverstyringPeriode);
    }
    public List<UUID> getOriginalReferanser() {
        return originalReferanser == null ? Collections.emptyList() : originalReferanser.values().stream().toList();
    }

    public Optional<UUID> getOriginalReferanseMedSammeSkjæringstidspunkt() {
        return originalReferanser == null ? Optional.empty() : Optional.ofNullable(originalReferanser.get(vilkårsperiode.getFomDato()));
    }

}
