package no.nav.k9.sak.behandlingskontroll.events;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.k9.sak.typer.AktørId;

/**
 * Event publiseres av {@link BehandlingskontrollTjeneste} når en {@link Behandling} endrer steg.
 * Kan brukes til å lytte på flyt i en Behandling og utføre logikk når det skjer.
 */
public class BehandlingStatusEvent implements BehandlingEvent {

    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingStatus nyStatus;
    private final BehandlingStatus gammelStatus;

    BehandlingStatusEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus, BehandlingStatus gammelStatus) {
        this.kontekst = kontekst;
        this.nyStatus = nyStatus;
        this.gammelStatus = gammelStatus;
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    public BehandlingStatus getNyStatus() {
        return nyStatus;
    }

    public BehandlingStatus getGammelStatus() {
        return gammelStatus;
    }

    static void validerRiktigStatus(BehandlingStatus nyStatus, BehandlingStatus expected) {
        if (!Objects.equals(expected, nyStatus)) {
            throw new IllegalArgumentException("Kan bare være " + expected + ", fikk: " + nyStatus);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + kontekst + //$NON-NLS-1$
            ", nyStatus=" + nyStatus + //$NON-NLS-1$
            ", gammelStatus=" + gammelStatus + //$NON-NLS-1$
            ">"; //$NON-NLS-1$
    }

    public static class BehandlingAvsluttetEvent extends BehandlingStatusEvent {
        BehandlingAvsluttetEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus, BehandlingStatus gammelStatus) {
            super(kontekst, nyStatus, gammelStatus);
            validerRiktigStatus(nyStatus, BehandlingStatus.AVSLUTTET);
        }
    }

    public static class BehandlingOpprettetEvent extends BehandlingStatusEvent {
        BehandlingOpprettetEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus, BehandlingStatus gammelStatus) {
            super(kontekst, nyStatus, gammelStatus);
            validerRiktigStatus(nyStatus, BehandlingStatus.OPPRETTET);
        }
    }

    @SuppressWarnings("unchecked")
    public static <V extends BehandlingStatusEvent> V nyEvent(BehandlingskontrollKontekst kontekst, BehandlingStatus nyStatus, BehandlingStatus gammelStatus) {
        if (BehandlingStatus.AVSLUTTET.equals(nyStatus)) {
            return (V) new BehandlingAvsluttetEvent(kontekst, nyStatus, gammelStatus);
        } else if (BehandlingStatus.OPPRETTET.equals(nyStatus)) {
            return (V) new BehandlingOpprettetEvent(kontekst, nyStatus, gammelStatus);
        } else {
            return (V) new BehandlingStatusEvent(kontekst, nyStatus, gammelStatus);
        }
    }
}
