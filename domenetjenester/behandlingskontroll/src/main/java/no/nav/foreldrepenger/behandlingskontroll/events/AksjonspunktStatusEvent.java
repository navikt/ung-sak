package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class AksjonspunktStatusEvent implements BehandlingEvent {
    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingStegType behandlingStegType;
    private List<Aksjonspunkt> aksjonspunkter;
    private AksjonspunktStatus fraStatus;
    private AksjonspunktStatus tilStatus;

    protected AksjonspunktStatusEvent(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType,
                                      List<Aksjonspunkt> aksjonspunkter, AksjonspunktStatus fraStatus, AksjonspunktStatus tilStatus) {
        super();
        this.kontekst = kontekst;
        this.behandlingStegType = behandlingStegType;
        this.aksjonspunkter = Collections.unmodifiableList(aksjonspunkter);
        this.fraStatus = fraStatus;
        this.tilStatus = tilStatus;
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    public List<Aksjonspunkt> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public Optional<AksjonspunktStatus> getFraStatus() {
        return Optional.ofNullable(fraStatus);
    }

    public AksjonspunktStatus getTilStatus() {
        return tilStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + aksjonspunkter + ", behandlingId="
                + getKontekst().getBehandlingId() + ", tilStatus=" + tilStatus + ">";
    }

}
