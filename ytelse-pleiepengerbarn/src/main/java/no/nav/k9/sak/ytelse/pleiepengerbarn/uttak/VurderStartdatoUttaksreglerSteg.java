package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_STARTDATO_UTTAKSREGLER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_STARTDATO_UTTAKSREGLER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VurderStartdatoUttaksreglerSteg implements BehandlingSteg {

    /**
     * Hensikten med denne datoen er å velge en spesifikk dato som alle som får automatisk vurdering av nye regler bruker.
     * Denne datoen vil føre til at alle perioder vurderes etter nye regler.
     * Samtidig har vi en enkel måte å finne disse på senere dersom vi heller bestemmer oss for å løse dette med et boolsk flagg.
     */
    private static final LocalDate EN_DATO_FOR_LENGE_SIDEN = LocalDate.of(2000, 1, 1);

    private BehandlingRepository behandlingRepository;
    private AksjonspunktUtlederNyeRegler aksjonspunktUtlederNyeRegler;
    private UttakNyeReglerRepository uttakNyeReglerRepository;

    VurderStartdatoUttaksreglerSteg() {
        // for proxy
    }

    @Inject
    public VurderStartdatoUttaksreglerSteg(BehandlingRepository behandlingRepository,
                                           AksjonspunktUtlederNyeRegler aksjonspunktUtlederNyeRegler,
                                           UttakNyeReglerRepository uttakNyeReglerRepository) {
        this.behandlingRepository = behandlingRepository;
        this.aksjonspunktUtlederNyeRegler = aksjonspunktUtlederNyeRegler;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.erRevurdering()) {
            uttakNyeReglerRepository.lagreDatoForNyeRegler(kontekst.getBehandlingId(), EN_DATO_FOR_LENGE_SIDEN);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Optional<AksjonspunktDefinisjon> aksjonspunktSetteDatoNyeRegler = aksjonspunktUtlederNyeRegler.utledAksjonspunktDatoForNyeRegler(behandling);
        return aksjonspunktSetteDatoNyeRegler.map(aksjonspunktDefinisjon -> BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon))).orElseGet(BehandleStegResultat::utførtUtenAksjonspunkter);
    }

}
