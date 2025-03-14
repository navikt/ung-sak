package no.nav.ung.sak.behandlingskontroll.events;

import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.ung.sak.typer.AktørId;

public class BehandlingTransisjonEvent implements BehandlingEvent {

    private final BehandlingskontrollKontekst kontekst;
    private TransisjonIdentifikator transisjonIdentifikator;
    private BehandlingStegTilstand fraTilstand;
    private BehandlingStegType tilStegType;
    private boolean erOverhopp;

    public BehandlingTransisjonEvent(BehandlingskontrollKontekst kontekst, TransisjonIdentifikator transisjonIdentifikator, BehandlingStegTilstand fraTilstand, BehandlingStegType tilStegType, boolean erOverhopp) {
        this.kontekst = kontekst;
        this.transisjonIdentifikator = transisjonIdentifikator;
        this.fraTilstand = fraTilstand;
        this.tilStegType = tilStegType;
        this.erOverhopp = erOverhopp;
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    public BehandlingskontrollKontekst getKontekst() {
        return kontekst;
    }

    public TransisjonIdentifikator getTransisjonIdentifikator() {
        return transisjonIdentifikator;
    }

    public Optional<BehandlingStegStatus> getFørsteStegStatus() {
        return Optional.ofNullable(fraTilstand).map(BehandlingStegTilstand::getBehandlingStegStatus);
    }

    public BehandlingStegType getFørsteSteg() {
        // siden hopper framover blir dette fraSteg
        return fraTilstand != null ? fraTilstand.getBehandlingSteg() : null;
    }

    public BehandlingStegType getSisteSteg() {
        // siden hopper framover blir dette tilSteg
        return tilStegType;
    }

    public boolean erOverhopp() {
        return erOverhopp;
    }
}
