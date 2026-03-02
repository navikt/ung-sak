package no.nav.ung.sak.web.app.tjenester.klage.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.klage.domenetjenester.KlageVurderingTjeneste;
import no.nav.ung.sak.kontrakt.klage.KlageVurderingResultatAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlagevurderingOppdaterer implements AksjonspunktOppdaterer<KlageVurderingResultatAksjonspunktDto> {
    private BehandlingRepository behandlingRepository;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private AksjonspunktRepository aksjonspunktRepository;
    private KlageHistorikkinnslagTjeneste historikkinnslagTjeneste;

    KlagevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlagevurderingOppdaterer(BehandlingRepository behandlingRepository,
                                    KlageVurderingTjeneste klageVurderingTjeneste,
                                    AksjonspunktRepository aksjonspunktRepository,
                                    KlageHistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(KlageVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        dto.valider();

        Behandling behandling = behandlingRepository.hentBehandling(param.getRef().getBehandlingId());
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());

        håndterKlageVurdering(dto, behandling);
        historikkinnslagTjeneste.opprettHistorikkinnslagVurdering(behandling, aksjonspunktDefinisjon, dto, dto.getBegrunnelse());

        boolean totrinn = håndterToTrinnsBehandling(behandling, aksjonspunktDefinisjon, dto.getKlageVurdering());
        return OppdateringResultat.builder().medTotrinnHvis(totrinn).build();
    }

    private void håndterKlageVurdering(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling) {
        Hjemmel hjemmel = Hjemmel.fraKode(dto.getKlageHjemmel());

        final KlageVurderingAdapter adapter = new KlageVurderingAdapter(
            dto.getKlageVurdering(),
            dto.getKlageMedholdArsak(),
            dto.getKlageVurderingOmgjoer(),
            dto.getBegrunnelse(),
            dto.getFritekstTilBrev(),
            hjemmel,
            null,
            KlageVurdertAv.VEDTAKSINSTANS);

        klageVurderingTjeneste.lagreVurdering(behandling, adapter);
    }

    private boolean håndterToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurderingType klageVurderingType) {
        if (KlageVurderingType.STADFESTE_YTELSESVEDTAK.getKode().equals(klageVurderingType.getKode())) {
            fjernToTrinnsBehandling(behandling, aksjonspunktDefinisjon);
            return false;
        } else {
            return true;
        }
    }

    private void fjernToTrinnsBehandling(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        if (aksjonspunkt.isToTrinnsBehandling()) {
            aksjonspunktRepository.fjernToTrinnsBehandlingKreves(aksjonspunkt);
        }
    }
}
