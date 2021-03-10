package no.nav.k9.sak.behandling.hendelse;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sikkerhet.context.SubjectHandler;

/**
 * Observerer Aksjonspunkt*Events og registrerer HistorikkInnslag for enkelte hendelser (eks. gjenoppta og behandling på vent)
 */
@ApplicationScoped
public class HistorikkInnslagForAksjonspunktEventObserver {

    private HistorikkRepository historikkRepository;
    private String systembruker;

    @Inject
    public HistorikkInnslagForAksjonspunktEventObserver(HistorikkRepository historikkRepository,
        /*
         * FIXME property vil være satt i produksjon, men ikke i tester. Uansett er løsningen ikke er god. Kan
         * heller bruker IdentType når det fikses.
         */
        @KonfigVerdi(value = "systembruker.username", required = false) String systembruker) {
        this.historikkRepository = historikkRepository;
        this.systembruker = systembruker;
    }

    /**
     * @param aksjonspunkterFunnetEvent
     */
    public void oppretteHistorikkForBehandlingPåVent(@Observes AksjonspunktStatusEvent aksjonspunkterFunnetEvent) {
        BehandlingskontrollKontekst ktx = aksjonspunkterFunnetEvent.getKontekst();
        for (Aksjonspunkt aksjonspunkt : aksjonspunkterFunnetEvent.getAksjonspunkter()) {
            if (aksjonspunkt.erOpprettet() && aksjonspunkt.getFristTid() != null) {
                LocalDateTime frist = aksjonspunkt.getFristTid();
                Venteårsak venteårsak = aksjonspunkt.getVenteårsak();
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(),
                    HistorikkinnslagType.BEH_VENT, frist, venteårsak);
            }
        }
    }

    private void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId,
                                                                      Long fagsakId,
                                                                      HistorikkinnslagType historikkinnslagType,
                                                                      LocalDateTime fristTid,
                                                                      Venteårsak venteårsak) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        if (fristTid != null) {
            builder.medHendelse(historikkinnslagType, fristTid.toLocalDate());
        } else {
            builder.medHendelse(historikkinnslagType);
        }
        if (venteårsak != null) {
            builder.medÅrsak(venteårsak);
        }
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        String brukerident = SubjectHandler.getSubjectHandler().getUid();
        historikkinnslag.setAktør(!Objects.equals(systembruker, brukerident) ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandlingId);
        historikkinnslag.setFagsakId(fagsakId);
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    public void oppretteHistorikkForGjenopptattBehandling(@Observes AksjonspunktStatusEvent aksjonspunkterFunnetEvent) {
        for (Aksjonspunkt aksjonspunkt : aksjonspunkterFunnetEvent.getAksjonspunkter()) {
            BehandlingskontrollKontekst ktx = aksjonspunkterFunnetEvent.getKontekst();
            if (aksjonspunkt.erUtført() && aksjonspunkt.getFristTid() != null) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), HistorikkinnslagType.BEH_GJEN, null, null);
            }
        }
    }
}
