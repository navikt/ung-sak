package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;


public interface EtterlysningOppretter {

    static EtterlysningOppretter finnTjeneste(Instance<EtterlysningOppretter> tjenester, BehandlingReferanse behandlingReferanse) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, behandlingReferanse.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Finner ingen EtterlysningOppretter for fagsakYtelseType=" + behandlingReferanse.getFagsakYtelseType()));
    }

    void opprettEtterlysninger(BehandlingReferanse behandlingReferanse);

}
