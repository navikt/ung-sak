package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(value = FORESLÅ_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository,
            ForeslåVedtakTjeneste foreslåVedtakTjeneste,
            @Any Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.ytelsespesifikkForeslåVedtak = ytelsespesifikkForeslåVedtak;
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
            // TODO: Rydd brev
//            formidlingDokumentdataTjeneste.ryddVedTilbakehopp(kontekst.getBehandlingId());
        }
    }
}
