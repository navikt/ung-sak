package no.nav.ung.sak.domene.behandling.steg.iverksettevedtak;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.IVERKSETT_VEDTAK;

import java.util.Optional;

import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.ung.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@BehandlingStegRef(value = IVERKSETT_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(IverksetteVedtakSteg.class);
    private Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    IverksetteVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakSteg(BehandlingRepositoryProvider repositoryProvider,
                                @Any Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett,
                                VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        this.tidligereBehandlingUnderIverksettelse = tidligereBehandlingUnderIverksettelse;
    }

    @Override
    public final BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Optional<BehandlingVedtak> fantVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId);
        if (fantVedtak.isEmpty()) {
            throw new IllegalStateException(String.format("Utviklerfeil: Kan ikke iverksette, behandling mangler vedtak %s", behandlingId));
        }
        BehandlingVedtak vedtak = fantVedtak.get();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (IverksettingStatus.IVERKSATT.equals(vedtak.getIverksettingStatus())) {
            log.info("Behandling {}: Iverksetting allerede fullført", kontekst.getBehandlingId());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Optional<Venteårsak> venteårsakOpt = kanBegynneIverksetting(behandling);
        if (venteårsakOpt.filter(Venteårsak.VENT_TIDLIGERE_BEHANDLING::equals).isPresent()) {
            log.info("Behandling {}: Iverksetting venter på annen behandling", behandlingId);
            // Bruker transisjon startet for "prøv utførSteg senere". Stegstatus VENTER betyr "under arbeid" (suspendert).
            // Behandlingsprosessen stopper og denne behandlingen blir plukket opp av avsluttBehandling.
            return BehandleStegResultat.startet();
        }
        log.info("Behandling {}: Iverksetter vedtak", behandlingId);
        iverksetter(behandling);

        return BehandleStegResultat.settPåVent();
    }

    @Override
    public final BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        log.info("Behandling {}: Iverksetting fullført", kontekst.getBehandlingId());
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        if (tidligereBehandlingUnderIverksettelse.vurder(behandling)) {
            opprettHistorikkinnslagNårIverksettelsePåVent(behandling);
            return Optional.of(Venteårsak.VENT_TIDLIGERE_BEHANDLING);
        }
        return Optional.empty();
    }


    private void opprettHistorikkinnslagNårIverksettelsePåVent(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Behandlingen venter på iverksettelse")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addLinje("Venter på iverksettelse av en tidligere behandling i denne saken")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }





    private void iverksetter(Behandling behandling) {
        FagsakYtelseTypeRef.Lookup.find(opprettProsessTaskIverksett, behandling.getFagsakYtelseType()).orElseThrow().opprettIverksettingstasker(behandling);
    }
}
