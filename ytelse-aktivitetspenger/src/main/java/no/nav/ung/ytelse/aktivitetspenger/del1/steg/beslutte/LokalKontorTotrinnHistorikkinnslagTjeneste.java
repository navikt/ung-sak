package no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class LokalKontorTotrinnHistorikkinnslagTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    LokalKontorTotrinnHistorikkinnslagTjeneste() {
        // CDI
    }

    @Inject
    public LokalKontorTotrinnHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                                                      TotrinnTjeneste totrinnTjeneste,
                                                      BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    public void lagHistorikkinnslagFattVedtak(Behandling behandling) {
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.LOKAL);

        if (erTotrinn) {
            Collection<Totrinnsvurdering> totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling, BehandlingDel.LOKAL);
            if (sendesTilbakeTilSaksbehandler(totrinnsvurderings)) {
                lagHistorikkInnslagVurderPåNytt(behandling, totrinnsvurderings);
                return;
            }
        }
        lagHistorikkInnslagVurderingGodkjent(behandling);
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !Boolean.TRUE.equals(a.isGodkjent()));
    }

    private void lagHistorikkInnslagVurderingGodkjent(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(utledAktør(behandling))
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.LOKALKONTOR_BESLUTTER_VILKÅR)
            .addLinje("Vurderingen ble godkjent")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private HistorikkAktør utledAktør(Behandling behandling) {
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.LOKAL);
        if (erTotrinn) {
            return HistorikkAktør.LOKALKONTOR_BESLUTTER;
        }
        return HistorikkAktør.VEDTAKSLØSNINGEN;
    }

    private void lagHistorikkInnslagVurderPåNytt(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_BESLUTTER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Sak retur")
            .medLinjer(lagTekstForHverTotrinnkontroll(medTotrinnskontroll))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }


    private static List<HistorikkinnslagLinjeBuilder> lagTekstForHverTotrinnkontroll(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .sorted(Comparator.comparing(ttv -> ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt()))
            .map(LokalKontorTotrinnHistorikkinnslagTjeneste::tilHistorikkinnslagTekst)
            .map(LokalKontorTotrinnHistorikkinnslagTjeneste::leggTilLinjeskift)
            .flatMap(Collection::stream)
            .toList();
    }

    private static List<HistorikkinnslagLinjeBuilder> tilHistorikkinnslagTekst(Totrinnsvurdering ttv) {
        var aksjonspunktNavn = ttv.getAksjonspunktDefinisjon().getNavn();
        if (Boolean.TRUE.equals(ttv.isGodkjent())) {
            return List.of(new HistorikkinnslagLinjeBuilder().bold(aksjonspunktNavn).bold("er godkjent"));
        }
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        linjer.add(new HistorikkinnslagLinjeBuilder().bold(aksjonspunktNavn).bold("må vurderes på nytt"));
        if (ttv.getBegrunnelse() != null) {
            linjer.add(new HistorikkinnslagLinjeBuilder().tekst("Kommentar:").tekst(ttv.getBegrunnelse()));
        }
        return linjer;
    }

    private static List<HistorikkinnslagLinjeBuilder> leggTilLinjeskift(List<HistorikkinnslagLinjeBuilder> eksistrendeLinjer) {
        var linjer = new ArrayList<>(eksistrendeLinjer);
        linjer.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        return linjer;
    }

}
