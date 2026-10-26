package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.PdpRequestMedBerørtePersonerForAuditlogg;
import no.nav.sif.abac.kontrakt.abac.AbacBehandlingDel;

public class UngSakPdpRequest extends PdpRequestMedBerørtePersonerForAuditlogg {

    private AbacBehandlingDel behandlingDel;

    public AbacBehandlingDel getBehandlingDel() {
        return behandlingDel;
    }

    public void setBehandlingDel(AbacBehandlingDel behandlingDel) {
        this.behandlingDel = behandlingDel;
    }
}
