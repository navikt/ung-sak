package no.nav.k9.sak.perioder;

import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface ForlengelseTjeneste {

    static ForlengelseTjeneste finnTjeneste(Instance<ForlengelseTjeneste> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(ForlengelseTjeneste.class, instances, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    /**
     * Utleder perioder til vurdering som skal behandles som forlengelse.
     * Dvs. ikke revurdere retten til ytelsen fra stp.
     *
     * @param referanse behandling ref
     * @param perioderTilVurdering periodene til vurdering i behandlingen
     * @param vilkårType vilkåret
     *
     * @return periodene som er å anse som forlengese
     */
    public NavigableSet<DatoIntervallEntitet> utledPerioderSomSkalBehandlesSomForlengelse(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårType vilkårType);
}
