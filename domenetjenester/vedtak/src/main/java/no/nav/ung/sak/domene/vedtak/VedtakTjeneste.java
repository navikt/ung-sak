package no.nav.ung.sak.domene.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.vedtak.regler.SatsEndring;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VedtakTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                          BehandlingRepository behandlingRepository,
                          TotrinnTjeneste totrinnTjeneste, BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    public void lagHistorikkinnslagFattVedtak(Behandling behandling) {
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.SENTRAL);

        if (erTotrinn) {
            Collection<Totrinnsvurdering> totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling, BehandlingDel.SENTRAL);
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
        boolean erTotrinn = behandlingAnsvarligRepository.erTotrinnsBehandling(behandling.getId(), BehandlingDel.SENTRAL);
        if (erTotrinn) {
            return HistorikkAktør.BESLUTTER;
        }
        var aksjonspunkt = behandling.getAksjonspunktForHvisFinnes(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.getKode());
        String ansvarligSaksbehandler = behandlingAnsvarligRepository.hentAnsvarligSaksbehandler(behandling.getId(), BehandlingDel.SENTRAL);
        if (aksjonspunkt.map(Aksjonspunkt::erUtført).orElse(false) && ansvarligSaksbehandler != null) {
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

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING;
        }

        return VedtakResultatType.AVSLAG;
    }

}
