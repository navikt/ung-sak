package no.nav.ung.sak.perioder;

import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

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
