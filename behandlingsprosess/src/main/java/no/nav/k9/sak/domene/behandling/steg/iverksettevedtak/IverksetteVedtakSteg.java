package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(IverksetteVedtakSteg.class);
    private Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett;
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse;
    private HistorikkRepository historikkRepository;
    private VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    IverksetteVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakSteg(BehandlingRepositoryProvider repositoryProvider,
                                @Any Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett,
                                IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse,
                                VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
        this.identifiserOverlappendeInfotrygdYtelse = identifiserOverlappendeInfotrygdYtelse;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
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
        førIverksetting(behandling, vedtak);
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
        HistorikkInnslagTekstBuilder delBuilder = new HistorikkInnslagTekstBuilder();
        delBuilder.medHendelse(HistorikkinnslagType.IVERKSETTELSE_VENT);
        delBuilder.medÅrsak(Venteårsak.VENT_TIDLIGERE_BEHANDLING);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.IVERKSETTELSE_VENT);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        delBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private void førIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        identifiserOverlappendeInfotrygdYtelse.vurderOgLagreEventueltOverlapp(behandling, behandlingVedtak);
    }

    private void iverksetter(Behandling behandling) {
        FagsakYtelseTypeRef.Lookup.find(opprettProsessTaskIverksett, behandling.getFagsakYtelseType()).orElseThrow().opprettIverksettingstasker(behandling);
    }
}
