package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.tjenester.FormidlingDokumentdataTjeneste;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste;
    private Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository,
            ForeslåVedtakTjeneste foreslåVedtakTjeneste,
            FormidlingDokumentdataTjeneste formidlingDokumentdataTjeneste,
            @Any Instance<YtelsespesifikkForeslåVedtak> ytelsespesifikkForeslåVedtak) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.formidlingDokumentdataTjeneste = formidlingDokumentdataTjeneste;
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
        if (!BehandlingStegType.FORESLÅ_VEDTAK.equals(tilSteg)) {
            formidlingDokumentdataTjeneste.ryddVedTilbakehopp(kontekst.getBehandlingId());
        }
    }
}
