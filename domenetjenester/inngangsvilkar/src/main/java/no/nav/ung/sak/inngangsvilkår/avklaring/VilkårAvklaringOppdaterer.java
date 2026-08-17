package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;

public interface VilkårAvklaringOppdaterer {

    static VilkårAvklaringOppdaterer finnTjeneste(Instance<VilkårAvklaringOppdaterer> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(VilkårAvklaringOppdaterer.class, instances, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

    void settAlleAvklaringerTilFerdig(long behandlingId);
}
