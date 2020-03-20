package no.nav.k9.sak.domene.medlem.kontrollerfakta;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.k9.sak.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.k9.sak.domene.medlem.impl.MedlemResultat;

@ApplicationScoped
public class AksjonspunktutlederForMedlemskap implements AksjonspunktUtleder {

    private BehandlingRepository behandlingRepository;
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;

    AksjonspunktutlederForMedlemskap() {
        //CDI
    }

    @Inject
    public AksjonspunktutlederForMedlemskap(BehandlingRepository behandlingRepository, UtledVurderingsdatoerForMedlemskapTjeneste tjeneste, VurderMedlemskapTjeneste vurderMedlemskapTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.tjeneste = tjeneste;
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        final var behandlingId = param.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Set<LocalDate> finnVurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);
        Set<MedlemResultat> resultat = new HashSet<>();
        if (!finnVurderingsdatoer.isEmpty()) {
            BehandlingReferanse ref = BehandlingReferanse.fra(behandling, param.getSkjæringstidspunkt());
            finnVurderingsdatoer.forEach(dato -> resultat.addAll(vurderMedlemskapTjeneste.vurderMedlemskap(ref, dato)));
        }
        if (!resultat.isEmpty()) {
            return List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP));
        }
        return List.of();
    }

}
