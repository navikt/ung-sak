package no.nav.k9.sak.behandling;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakEvent;
import no.nav.k9.sak.typer.AktørId;

/**
 * Event publiseres når Fagsak endrer status
 */
public class FagsakStatusEvent implements FagsakEvent {

    private Long fagsakId;
    private FagsakStatus forrigeStatus;
    private FagsakStatus nyStatus;
    private AktørId aktørId;
    private FagsakYtelseType ytelseType;

    public FagsakStatusEvent(Long fagsakId, AktørId aktørId, FagsakYtelseType ytelseType, FagsakStatus forrigeStatus, FagsakStatus nyStatus) {
        super();
        this.fagsakId = fagsakId;
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
        this.forrigeStatus = forrigeStatus;
        this.nyStatus = nyStatus;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public Long getFagsakId() {
        return fagsakId;
    }

    public FagsakStatus getForrigeStatus() {
        return forrigeStatus;
    }

    public FagsakStatus getNyStatus() {
        return nyStatus;
    }
    
    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + fagsakId + //$NON-NLS-1$
            ", forrigeStatus=" + forrigeStatus + //$NON-NLS-1$
            ", nyStatus=" + nyStatus + //$NON-NLS-1$
            ", ytelseType=" + ytelseType + //$NON-NLS-1$
            ">"; //$NON-NLS-1$
    }
}
