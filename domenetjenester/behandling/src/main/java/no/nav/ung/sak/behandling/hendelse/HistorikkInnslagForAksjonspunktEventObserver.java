package no.nav.ung.sak.behandling.hendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Observerer Aksjonspunkt*Events og registrerer HistorikkInnslag for enkelte hendelser (eks. gjenoppta og behandling på vent)
 */
@ApplicationScoped
public class HistorikkInnslagForAksjonspunktEventObserver {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private String systembruker;
    private String appName;

    @Inject
    public HistorikkInnslagForAksjonspunktEventObserver(
        HistorikkinnslagRepository historikkinnslagRepository,
        /*
         * FIXME property vil være satt i produksjon, men ikke i tester. Uansett er løsningen ikke er god. Kan
         * heller bruker IdentType når det fikses.
         */
        @KonfigVerdi(value = "systembruker.username", required = false) String systembruker,
        @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "ung-sak") String appName) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.systembruker = systembruker;
        this.appName = appName;
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
                    "Behandlingen er satt på vent", frist, venteårsak, aksjonspunkt.getVenteårsakVariant());
            }
        }
    }

    private void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId,
                                                                      Long fagsakId,
                                                                      String tittel,
                                                                      LocalDateTime fristTid,
                                                                      Venteårsak venteårsak,
                                                                      String venteårsakVariant) {
        var historikkinnslagBuilder = new Historikkinnslag.Builder();
        if (fristTid != null) {
            historikkinnslagBuilder.medTittel(tittel + " til " + HistorikkinnslagLinjeBuilder.format(fristTid.toLocalDate()));
        } else {
            historikkinnslagBuilder.medTittel(tittel);
        }
        if (venteårsak != null) {
            historikkinnslagBuilder.addLinje(venteårsak.getNavn());
        }
        if (venteårsakVariant != null) {
            historikkinnslagBuilder.addLinje(venteårsakVariant);
        }
        var erSystemBruker = erSystembruker();
        historikkinnslagBuilder
            .medAktør(erSystemBruker ? HistorikkAktør.VEDTAKSLØSNINGEN : HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandlingId)
            .medFagsakId(fagsakId);
        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
    }


    private boolean erSystembruker() {
        var innloggetBrukerId = SubjectHandler.getSubjectHandler().getUid();
        return Objects.equals(systembruker, innloggetBrukerId) || Objects.equals(appName, innloggetBrukerId);
    }

    public void oppretteHistorikkForGjenopptattBehandling(@Observes AksjonspunktStatusEvent aksjonspunkterFunnetEvent) {
        for (Aksjonspunkt aksjonspunkt : aksjonspunkterFunnetEvent.getAksjonspunkter()) {
            BehandlingskontrollKontekst ktx = aksjonspunkterFunnetEvent.getKontekst();
            if (aksjonspunkt.erUtført() && aksjonspunkt.getFristTid() != null) {
                opprettHistorikkinnslagForVenteFristRelaterteInnslag(ktx.getBehandlingId(), ktx.getFagsakId(), "Behandlingen er gjenopptatt", null, null, null);
            }
        }
    }
}
