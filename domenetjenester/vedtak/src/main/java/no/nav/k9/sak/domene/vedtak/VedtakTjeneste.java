package no.nav.k9.sak.domene.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class VedtakTjeneste {

    private HistorikkRepository historikkRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingRepository behandlingRepository;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(HistorikkRepository historikkRepository,
                          BehandlingRepository behandlingRepository,
                          TotrinnTjeneste totrinnTjeneste) {
        this.historikkRepository = historikkRepository;
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
        var ref = BehandlingReferanse.fra(behandling);
        boolean erUendretUtfall = ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret();

        HistorikkinnslagType historikkinnslagType = erUendretUtfall ? HistorikkinnslagType.UENDRET_UTFALL : HistorikkinnslagType.VEDTAK_FATTET;
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(historikkinnslagType)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        if (!erUendretUtfall) {
            tekstBuilder.medResultat(utledVedtakResultatType(behandling));
        }
        Historikkinnslag innslag = new Historikkinnslag();
        var aktør = utledAktør(behandling);
        innslag.setAktør(aktør);
        if (HistorikkAktør.SAKSBEHANDLER.equals(aktør)) {
            innslag.setOpprettetAv(behandling.getAnsvarligSaksbehandler());
        }
        innslag.setType(historikkinnslagType);
        innslag.setBehandling(behandling);
        tekstBuilder.build(innslag);

        historikkRepository.lagre(innslag);
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
        Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering = new HashMap<>();
        List<HistorikkinnslagTotrinnsvurdering> vurderingUtenLenke = new ArrayList<>();

        HistorikkInnslagTekstBuilder delBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SAK_RETUR);

        for (Totrinnsvurdering ttv : medTotrinnskontroll) {
            HistorikkinnslagTotrinnsvurdering totrinnsVurdering = lagHistorikkinnslagTotrinnsvurdering(ttv);
            LocalDateTime sistEndret = ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt();
            totrinnsVurdering.setAksjonspunktSistEndret(sistEndret);
            SkjermlenkeType skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(ttv.getAksjonspunktDefinisjon());
            if (skjermlenkeType != null && !SkjermlenkeType.UDEFINERT.equals(skjermlenkeType)) {
                vurdering.computeIfAbsent(skjermlenkeType, k -> new ArrayList<>()).add(totrinnsVurdering);
            } else {
                vurderingUtenLenke.add(totrinnsVurdering);
            }
        }
        delBuilder.medTotrinnsvurdering(vurdering, vurderingUtenLenke);

        historikkRepository.lagre(lagHistorikkinnslag(behandling, HistorikkinnslagType.SAK_RETUR, delBuilder));
    }

    private Historikkinnslag lagHistorikkinnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType,
                                                 HistorikkInnslagTekstBuilder builder) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(HistorikkAktør.BESLUTTER);
        historikkinnslag.setType(historikkinnslagType);
        builder.build(historikkinnslag);

        return historikkinnslag;
    }

    private HistorikkinnslagTotrinnsvurdering lagHistorikkinnslagTotrinnsvurdering(Totrinnsvurdering ttv) {
        HistorikkinnslagTotrinnsvurdering totrinnsVurdering = new HistorikkinnslagTotrinnsvurdering();
        totrinnsVurdering.setAksjonspunktDefinisjon(ttv.getAksjonspunktDefinisjon());
        totrinnsVurdering.setBegrunnelse(ttv.getBegrunnelse());
        totrinnsVurdering.setGodkjent(Boolean.TRUE.equals(ttv.isGodkjent()));
        return totrinnsVurdering;
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
