package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef("BT-004")
class UtvidetRettRevurderingBehandlingsresultatutleder implements no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder {

    private BehandlingRepository behandlingRepository;

    @SuppressWarnings("unused")
    private VilkårResultatRepository vilkårResultatRepository;

    protected UtvidetRettRevurderingBehandlingsresultatutleder() {
    }

    @Inject
    public UtvidetRettRevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider) {

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    @Override
    public void bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());
        Long originalBehandlingId = revurdering.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Mangler originalBehandlingId for behandling: " + revurdering.getId()));
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        doBestemBehandlingsresultatForRevurdering(revurdering, originalBehandling);
    }

    private void doBestemBehandlingsresultatForRevurdering(Behandling revurdering, Behandling originalBehandling) {

        // TODO: håndter innvilget endring eller avslått
        revurdering.setBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
    }

}
