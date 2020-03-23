package no.nav.k9.sak.domene.behandling.steg.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.økonomi.simulering.tjeneste.SimulerInntrekkSjekkeTjeneste;

@BehandlingStegRef(kode = "FVEDSTEG")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class FatteVedtakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private FatteVedtakTjeneste fatteVedtakTjeneste;
    private SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste;

    FatteVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakSteg(BehandlingRepositoryProvider repositoryProvider,
                    FatteVedtakTjeneste fatteVedtakTjeneste,
                    SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fatteVedtakTjeneste = fatteVedtakTjeneste;
        this.simulerInntrekkSjekkeTjeneste = simulerInntrekkSjekkeTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        return fatteVedtakTjeneste.fattVedtak(kontekst, behandling);
    }
}
