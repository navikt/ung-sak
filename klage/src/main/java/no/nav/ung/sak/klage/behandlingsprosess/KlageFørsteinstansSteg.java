package no.nav.ung.sak.klage.behandlingsprosess;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class KlageFørsteinstansSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private FritekstRepository fritekstRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    public KlageFørsteinstansSteg(){
        // For CDI proxy
    }

    @Inject
    public KlageFørsteinstansSteg(BehandlingRepository behandlingRepository,
                                  KlageRepository klageRepository,
                                  FritekstRepository fritekstRepository,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.fritekstRepository = fritekstRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (klageRepository.hentKlageUtredning(kontekst.getBehandlingId()).erKlageAvvist()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_VEDTAKSINSTANS);
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if(!Objects.equals(BehandlingStegType.FATTE_VEDTAK, sisteSteg)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

            var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
            klageutredning.fjernKlageVurderingVedtaksinstans();

            endreAnsvarligEnhetTilFørsteinstansVedTilbakeføringOgLagreHistorikkinnslag(behandling);

            klageRepository.lagre(klageutredning);
        }
    }

    private void endreAnsvarligEnhetTilFørsteinstansVedTilbakeføringOgLagreHistorikkinnslag(Behandling behandling) {
        var opprinneligEnhet = klageRepository.hentKlageUtredning(behandling.getId())
            .getOpprinneligBehandlendeEnhet();
        if (behandling.getBehandlendeEnhet() != null && behandling.getBehandlendeEnhet().equals(opprinneligEnhet)) {
            return;
        }
        var tilEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }
}
