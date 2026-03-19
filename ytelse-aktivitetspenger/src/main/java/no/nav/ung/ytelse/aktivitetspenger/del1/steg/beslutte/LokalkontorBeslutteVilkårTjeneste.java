package no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
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
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

@ApplicationScoped
public class LokalkontorBeslutteVilkårTjeneste {

    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    LokalkontorBeslutteVilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorBeslutteVilkårTjeneste(TotrinnTjeneste totrinnTjeneste,
                                             BehandlingAnsvarligRepository behandlingAnsvarligRepository,
                                             HistorikkinnslagRepository historikkinnslagRepository) {
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public BehandleStegResultat besluttVilkår(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            throw new IllegalStateException("Kun ytelsesbehandling er støttet her p.t.");
        }

        if (behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.LOKAL)) {
            final var fatterVedtakAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.LOKALKONTOR_BESLUTTER_VILKÅR);

            // Dersom vi ikke har fatter vedtak aksjonspunkt eller allerede har opprettet aksjonspunkt og behandlingen er flagget som totrinnsbehandling returnerer vi med aksjonspunkt og går videre til steg-ut
            if (fatterVedtakAksjonspunkt.filter(Aksjonspunkt::erUtført).isEmpty()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.LOKALKONTOR_BESLUTTER_VILKÅR));
            }

            Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling).stream()
                .filter(totrinnsvurdering -> totrinnsvurdering.getAksjonspunktDefinisjon().getBehandlingDel() == BehandlingDel.LOKAL)
                .toList();
            // Sjekker om vi har minst en ikke godkjent vurdering og om behandlingen skal flyttes tilbake
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = finnIkkeGodkjenteVurderinger(totrinnaksjonspunktvurderinger);
                // Flytter behandling tilbake til første ikke-godkjente aksjonspunkt
                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            } else if (harUtførtAksjonspunktOgGodkjentAlleVurderinger(fatterVedtakAksjonspunkt.get(), totrinnaksjonspunktvurderinger)) {
                // Dersom alle vurderinger er godkjent og aksjonspunktet er utført går vi videre
            } else {
                throw new IllegalStateException("Kunne ikke fatte vedtak. Hadde aksjonspunkt med status " + fatterVedtakAksjonspunkt.get().getStatus() + " og totrinnsvurderinger: " + totrinnaksjonspunktvurderinger);
            }
        } else {
            totrinnTjeneste.deaktiverTotrinnaksjonspunktvurderinger(behandling, BehandlingDel.LOKAL);
            lagHistorikkinnslagBeslutteVedtak(behandling);
        }


        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static boolean harUtførtAksjonspunktOgGodkjentAlleVurderinger(Aksjonspunkt fatterVedtakAksjonspunkt, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return fatterVedtakAksjonspunkt.getStatus() == AksjonspunktStatus.UTFØRT && !totrinnaksjonspunktvurderinger.isEmpty() && totrinnaksjonspunktvurderinger.stream().allMatch(Totrinnsvurdering::isGodkjent);
    }

    private static List<AksjonspunktDefinisjon> finnIkkeGodkjenteVurderinger(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return totrinnaksjonspunktvurderinger.stream()
            .filter(a -> !a.isGodkjent())
            .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
            .collect(Collectors.toList());
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

    public void lagHistorikkinnslagBeslutteVedtak(Behandling behandling) {
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.LOKAL);

        if (erTotrinn) {
            Collection<Totrinnsvurdering> totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling).stream()
                .filter(totrinnsvurdering -> totrinnsvurdering.getAksjonspunktDefinisjon().getBehandlingDel() == BehandlingDel.LOKAL)
                .toList();;
            if (sendesTilbakeTilSaksbehandler(totrinnsvurderings)) {
                lagHistorikkInnslagVurderPåNytt(behandling, totrinnsvurderings);
                return;
            }
        }
        lagHistorikkInnslagBesluttetVilkår(behandling);
    }

    private void lagHistorikkInnslagBesluttetVilkår(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(utledAktør(behandling))
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.LOKALKONTOR_BESLUTTER_VILKÅR)
            .addLinje("Vilkår som behandles ved lokalkontor er besluttet")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private HistorikkAktør utledAktør(Behandling behandling) {
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId());
        return erTotrinn ? HistorikkAktør.LOKALKONTOR_BESLUTTER : HistorikkAktør.VEDTAKSLØSNINGEN;
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
            .map(LokalkontorBeslutteVilkårTjeneste::tilHistorikkinnslagTekst)
            .map(LokalkontorBeslutteVilkårTjeneste::leggTilLinjeskift)
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
