package no.nav.ung.sak.behandlingskontroll.events;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.ung.sak.typer.AktørId;

/**
 * Event publiseres av {@link BehandlingskontrollTjeneste} når en {@link Behandling} endrer steg.
 * Kan brukes til å lytte på flyt i en Behandling og utføre logikk når det skjer.
 */
public class BehandlingStegStatusEvent implements BehandlingEvent {

    private BehandlingskontrollKontekst kontekst;

    private BehandlingStegStatus nyStatus;

    private BehandlingStegType stegType;

    private BehandlingStegStatus forrigeStatus;

    public BehandlingStegStatusEvent(BehandlingskontrollKontekst kontekst, BehandlingStegType stegType, BehandlingStegStatus forrigeStatus,
            BehandlingStegStatus nyStatus) {
        super();
        this.kontekst = kontekst;
        this.stegType = stegType;
        this.forrigeStatus = forrigeStatus;
        this.nyStatus = nyStatus;
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public BehandlingStegType getStegType() {
        return stegType;
    }

    public BehandlingStegStatus getForrigeStatus() {
        return forrigeStatus;
    }

    public BehandlingStegStatus getNyStatus() {
        return nyStatus;
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + kontekst + //$NON-NLS-1$
                ", steg=" + stegType + //$NON-NLS-1$
                ", forrigeStatus=" + forrigeStatus + //$NON-NLS-1$
                ", nyStatus=" + nyStatus + //$NON-NLS-1$
                ">"; //$NON-NLS-1$
    }
}
