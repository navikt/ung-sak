package no.nav.k9.sak.perioder;

import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPerioderTilVurderingTjeneste {

    default NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(@SuppressWarnings("unused") Long behandlingId) {
        return new TreeSet<>();
    }

    default NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(@SuppressWarnings("unused") Long behandlingId) {
        return null;
    }

    NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType);

    /**
     * Utleder perioder som har trengs reberegnet pga revurdering i uttakssteget
     *
     * @param referanse behandlingen
     * @return set med perioder
     */
    default NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        return new TreeSet<>();
    }

    Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId);

    int maksMellomliggendePeriodeAvstand();

    default KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }
}
