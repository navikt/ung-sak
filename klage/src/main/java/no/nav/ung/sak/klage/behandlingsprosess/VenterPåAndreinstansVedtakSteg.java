package no.nav.ung.sak.klage.behandlingsprosess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@BehandlingStegRef(BehandlingStegType.OVERFØRT_NK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VenterPåAndreinstansVedtakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;

    VenterPåAndreinstansVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    public VenterPåAndreinstansVedtakSteg(BehandlingRepositoryProvider repositoryProvider,
                                          KlageRepository klageRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.klageRepository = klageRepository;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var klageVurdering = klageRepository.hentKlageUtredning(behandling.getId());
        var skalOversendesKabal = klageVurdering.getKlageVurderingType(KlageVurdertAv.VEDTAKSINSTANS)
            .map(KlageVurderingType.STADFESTE_YTELSESVEDTAK::equals)
            .orElse(false);

        behandling.nullstillToTrinnsBehandling();

        if (skalOversendesKabal) {
            // Sett på vent. Steget tas av vent når Kabal sender tilbake hendelse om utfall
            var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_OVERFØRT_NK,
                Venteårsak.OVERSENDT_KABAL,
                LocalDateTime.now().plusYears(5)); // Laaang ventefrist - skal aldri gå videre
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        } else {
            // Medhold, henleggelse og avvist går utenom NK og direkte til foreslå vedtak
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.FATTE_VEDTAK, sisteSteg)) {
            var klageutredning = klageRepository.hentKlageUtredning(kontekst.getBehandlingId());
            klageutredning.fjernKlageVurdering(KlageVurdertAv.KLAGEINSTANS);
            klageRepository.lagre(klageutredning);
        }
    }
}
