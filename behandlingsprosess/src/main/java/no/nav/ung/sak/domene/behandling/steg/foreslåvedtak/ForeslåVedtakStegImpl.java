package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.FormidlingTjeneste;

import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;

@BehandlingStegRef(value = FORESLÅ_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak;
    private FormidlingTjeneste formidlingTjeneste;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository,
                          ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                          @Any Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak, FormidlingTjeneste formidlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.ytelsespesifikkForeslåVedtak = ytelsespesifikkForeslåVedtak;
        this.formidlingTjeneste = formidlingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        final Optional<BehandleStegResultat> ytelsespesifikkForeslåVedtakResultat = hentAlternativForeslåVedtak(behandling)
            .map(afv -> afv.run(BehandlingReferanse.fra(behandling)));
        if (ytelsespesifikkForeslåVedtakResultat.isPresent()) {
            return ytelsespesifikkForeslåVedtakResultat.get();
        }

        return foreslåVedtakTjeneste.foreslåVedtak(behandling, kontekst);
    }

    private Optional<? extends YtelsespesifikkForeslåVedtak> hentAlternativForeslåVedtak(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(ytelsespesifikkForeslåVedtak, behandling.getFagsakYtelseType());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!FORESLÅ_VEDTAK.equals(tilSteg)) {
            formidlingTjeneste.ryddVedTilbakeHopp(kontekst.getBehandlingId());
        }
    }
}
