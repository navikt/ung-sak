package no.nav.ung.sak.domene.behandling.steg.klage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
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
        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FRISINN)) {
            // Legacy: For Frisinn-klager overføres ikke klagen til Kabal
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var klageVurdering = klageRepository.hentKlageUtredning(behandling.getId());
        var harMedholdFraFørsteinstans = klageVurdering.getKlageVurderingType(KlageVurdertAv.NAY)
            .map(KlageVurderingType.MEDHOLD_I_KLAGE::equals)
            .orElse(false);

        if (harMedholdFraFørsteinstans) {
            // Medhold går utenom NK og direkte til vedtak
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // Sett på vent. Steget tas av vent når Kabal sender tilbake hendelse om utfall
        var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
            AksjonspunktDefinisjon.AUTO_OVERFØRT_NK,
            Venteårsak.OVERSENDT_KABAL,
            LocalDateTime.now().plusYears(5)); // Laaang ventefrist - skal aldri gå videre
        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
    }
}
