package no.nav.k9.sak.perioder;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.Instance;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

public interface VilkårsPerioderTilVurderingTjeneste {

    static VilkårsPerioderTilVurderingTjeneste finnTjeneste(Instance<VilkårsPerioderTilVurderingTjeneste> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, instances, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    default NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(@SuppressWarnings("unused") Long behandlingId) {
        return new TreeSet<>();
    }

    default NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(@SuppressWarnings("unused") Long behandlingId) {
        return null;
    }

    NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType);

    default NavigableSet<DatoIntervallEntitet> utledFraDefinerendeVilkår(Long behandlingId) {
        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();
        for (VilkårType vilkårType : this.definerendeVilkår()) {
            NavigableSet<DatoIntervallEntitet> perioderForVilkår = this.utled(behandlingId, vilkårType);
            var perioderSomTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioderForVilkår);
            tidslinje = tidslinje.crossJoin(perioderSomTidslinje, StandardCombinators::alwaysTrueForMatch);
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje.compress());
    }
    Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId);

    int maksMellomliggendePeriodeAvstand();

    /**
     * Utleder perioder som har trengs reberegnet pga revurdering i uttakssteget
     *
     * @param referanse behandlingen
     * @return set med perioder
     */
    default NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        return new TreeSet<>();
    }

    /**
     * Utleder perioder som skal revurders pga endring i annen sak på samme pleietrengende
     *
     * @param referanse behandlingen
     * @return set med perioder
     */
    default NavigableSet<PeriodeMedÅrsak> utledRevurderingPerioder(BehandlingReferanse referanse) {
        return new TreeSet<>();
    }

    default KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }

    /**
     * Definerer perioden(e) til behandling for brev
     *
     * @return vilkårene
     */
    default Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }
}
