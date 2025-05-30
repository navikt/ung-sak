package no.nav.ung.sak.domene.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.*;

@ApplicationScoped
public class VedtakTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                          BehandlingRepository behandlingRepository,
                          TotrinnTjeneste totrinnTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void lagHistorikkinnslagFattVedtak(Behandling behandling) {
        if (behandling.isToTrinnsBehandling()) {
            Collection<Totrinnsvurdering> totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            if (sendesTilbakeTilSaksbehandler(totrinnsvurderings)) {
                lagHistorikkInnslagVurderPåNytt(behandling, totrinnsvurderings);
                return;
            }
        }
        lagHistorikkInnslagVedtakFattet(behandling);
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !Boolean.TRUE.equals(a.isGodkjent()));
    }

    private void lagHistorikkInnslagVedtakFattet(Behandling behandling) {
        boolean erUendretUtfall = behandling.getBehandlingResultatType().isBehandlingsresultatIkkeEndret();
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(utledAktør(behandling))
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.VEDTAK)
            .addLinje(erUendretUtfall ? "Uendret utfall" : String.format("Vedtak er fattet: %s", utledVedtakResultatType(behandling).getNavn()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private HistorikkAktør utledAktør(Behandling behandling) {
        if (behandling.isToTrinnsBehandling()) {
            return HistorikkAktør.BESLUTTER;
        }
        var aksjonspunkt = behandling.getAksjonspunktForHvisFinnes(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.getKode());
        if (aksjonspunkt.map(Aksjonspunkt::erUtført).orElse(false) && !Objects.equals(null, behandling.getAnsvarligSaksbehandler())) {
            return HistorikkAktør.SAKSBEHANDLER;
        }
        return HistorikkAktør.VEDTAKSLØSNINGEN;
    }

    private void lagHistorikkInnslagVurderPåNytt(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.BESLUTTER)
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
            .map(VedtakTjeneste::tilHistorikkinnslagTekst)
            .map(VedtakTjeneste::leggTilLinjeskift)
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
    public VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        BehandlingResultatType resultatType = behandling.getBehandlingResultatType();
        return utledVedtakResultatType(behandling, resultatType);
    }

    private VedtakResultatType utledVedtakResultatType(Behandling behandling, BehandlingResultatType resultatType) {
        if (BehandlingResultatType.INGEN_ENDRING == resultatType) {
            Optional<Long> originalBehandlingOpt = behandling.getOriginalBehandlingId();
            if (originalBehandlingOpt.isPresent()) {
                var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingOpt.get());
                if (originalBehandling.getBehandlingResultatType() != BehandlingResultatType.IKKE_FASTSATT) {
                    return utledVedtakResultatType(originalBehandling);
                }
            }
        }
        if (BehandlingResultatType.DELVIS_INNVILGET == resultatType) {
            return VedtakResultatType.DELVIS_INNVILGET;
        }
        if (BehandlingResultatType.getInnvilgetKoder().contains(resultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.OPPHØR == resultatType) {
            return VedtakResultatType.OPPHØR;
        }
        return VedtakResultatType.AVSLAG;
    }

}
