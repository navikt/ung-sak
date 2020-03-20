package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository, ForeslåVedtakTjeneste foreslåVedtakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        return foreslåVedtakTjeneste.foreslåVedtak(behandling, kontekst);
    }
}
