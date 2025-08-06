package no.nav.ung.sak.web.app.tjenester.klage.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageVurdering;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.klage.KlageVurderingTjeneste;
import no.nav.ung.sak.kontrakt.klage.KlageVurderingResultatAksjonspunktDto;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;

import java.util.Objects;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlagevurderingOppdaterer implements AksjonspunktOppdaterer<KlageVurderingResultatAksjonspunktDto> {
    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private AksjonspunktRepository aksjonspunktRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    KlagevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlagevurderingOppdaterer(HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                    BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste,
                                    KlageVurderingTjeneste klageVurderingTjeneste, AksjonspunktRepository aksjonspunktRepository,
                                    BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(KlageVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        boolean totrinn = håndterToTrinnsBehandling(behandling, aksjonspunktDefinisjon, dto.getKlageVurdering());

        håndterKlageVurdering(dto, behandling, aksjonspunktDefinisjon);

        opprettHistorikkinnslag(behandling, aksjonspunktDefinisjon, dto);

        return OppdateringResultat.builder().medTotrinnHvis(totrinn).build();
    }

    private void håndterKlageVurdering(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Hjemmel hjemmel = Hjemmel.fraKode(dto.getKlageHjemmel());

        final KlageVurderingAdapter adapter = new KlageVurderingAdapter(
            dto.getKlageVurdering(),
            dto.getKlageMedholdArsak(),
            dto.getKlageVurderingOmgjoer(),
            dto.getBegrunnelse(),
            dto.getFritekstTilBrev(),
            hjemmel,
            KlageVurdertAv.NAY);

        klageVurderingTjeneste.lagreVurdering(behandling, adapter);
    }

    private boolean håndterToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurdering klageVurdering) {
        if (erNfpAksjonspunkt(aksjonspunktDefinisjon)) {
            if (!KlageVurdering.STADFESTE_YTELSESVEDTAK.getKode()
                .equals(klageVurdering.getKode())) {
                return true;
            } else {
                // Må fjerne totrinnsbehandling i tilfeller hvor totrinn er satt for NFP (klagen ikke er innom NK),
                // beslutter sender behandlingen tilbake til NFP, og NFP deretter gjør et valgt som sender
                // behandlingen til NK. Da skal ikke aksjonspunkt NFP totrinnsbehandles.
                fjernToTrinnsBehandling(behandling, aksjonspunktDefinisjon);
            }
        }
        return false;
    }

    private void fjernToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        if (aksjonspunkt.isToTrinnsBehandling()) {
            aksjonspunktRepository.fjernToTrinnsBehandlingKreves(aksjonspunkt);
        }
    }

    private void opprettHistorikkinnslag(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurderingResultatAksjonspunktDto dto) {
        KlageVurdering klageVurdering = KlageVurdering.fraKode(dto.getKlageVurdering().getKode());
        KlageVurderingOmgjør klageVurderingOmgjør = dto.getKlageVurderingOmgjoer() != null
            ? KlageVurderingOmgjør.fraKode(dto.getKlageVurderingOmgjoer().getKode()) : null;
        boolean erNfpAksjonspunkt = erNfpAksjonspunkt(aksjonspunktDefinisjon);
//        HistorikkinnslagType historikkinnslagType = erNfpAksjonspunkt ? HistorikkinnslagType.KLAGE_BEH_NFP : HistorikkinnslagType.KLAGE_BEH_NK;
//        Kodeverdi årsak = null;
//        if (dto.getKlageMedholdArsak() != null) {
//            årsak = dto.getKlageMedholdArsak();
//        } else if (dto.getKlageAvvistArsak() != null) {
//            årsak = dto.getKlageAvvistArsak();
//        }
//
//        HistorikkResultatType resultat = konverterKlageVurderingTilResultatType(klageVurdering, erNfpAksjonspunkt, klageVurderingOmgjør);
//        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder();
//        if (erNfpAksjonspunkt) {
//            historiebygger.medEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_NFP, null, resultat.getNavn());
//        } else {
//            historiebygger.medEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_KA, null, resultat.getNavn());
//        }
//        if (årsak != null) {
//            historiebygger.medEndretFelt(HistorikkEndretFeltType.KLAGE_OMGJØR_ÅRSAK, null, årsak.getNavn());
//        }
//        var skjermlenkeType = getSkjermlenkeType(dto.getKode());
//        historiebygger.medBegrunnelse(dto.getBegrunnelse());
//        historiebygger.medSkjermlenke(skjermlenkeType);
//
//        Historikkinnslag innslag = new Historikkinnslag();
//        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
//        innslag.setType(historikkinnslagType);
//        innslag.setBehandlingId(behandling.getId());
//        historiebygger.build(innslag);
//
//        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }
//
//    private SkjermlenkeType getSkjermlenkeType(String apKode) {
//        return apKode.equals(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode()) ? SkjermlenkeType.KLAGE_BEH_NFP : SkjermlenkeType.KLAGE_BEH_NK;
//    }
//
//    private HistorikkResultatType konverterKlageVurderingTilResultatType(KlageVurdering vurdering, boolean erNfpAksjonspunkt, KlageVurderingOmgjør klageVurderingOmgjør) {
//        if (KlageVurdering.AVVIS_KLAGE.equals(vurdering)) {
//            return HistorikkResultatType.AVVIS_KLAGE;
//        }
//        if (KlageVurdering.MEDHOLD_I_KLAGE.equals(vurdering)) {
//            if (KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
//                return HistorikkResultatType.DELVIS_MEDHOLD_I_KLAGE;
//            }
//            if (KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE.equals(klageVurderingOmgjør)) {
//                return HistorikkResultatType.UGUNST_MEDHOLD_I_KLAGE;
//            }
//            return HistorikkResultatType.MEDHOLD_I_KLAGE;
//        }
//        if (KlageVurdering.OPPHEVE_YTELSESVEDTAK.equals(vurdering)) {
//            return HistorikkResultatType.OPPHEVE_VEDTAK;
//        }
//        if (KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(vurdering)) {
//            return HistorikkResultatType.KLAGE_HJEMSENDE_UTEN_OPPHEVE;
//        }
//        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
//            if (erNfpAksjonspunkt) {
//                return HistorikkResultatType.OPPRETTHOLDT_VEDTAK;
//            }
//            return HistorikkResultatType.STADFESTET_VEDTAK;
//        }
//        return null;
//    }
//
//    private void byttBehandlendeEnhet(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
//        KlageVurdering klageVurdering = KlageVurdering.fraKode(dto.getKlageVurdering().getKode());
//        if (erNfpAksjonspunkt(aksjonspunktDefinisjon) && klageVurdering.equals(KlageVurdering.STADFESTE_YTELSESVEDTAK)) {
//
//            behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandling.getId(), behandlendeEnhetTjeneste.getKlageInstans(),
//                "", //Det er ikke behov for en begrunnelse i dette tilfellet.
//                HistorikkAktør.VEDTAKSLØSNINGEN);
//
//        }
//    }

    private boolean erNfpAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return Objects.equals(aksjonspunktDefinisjon.getKode(), AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.getKode());
    }
}
