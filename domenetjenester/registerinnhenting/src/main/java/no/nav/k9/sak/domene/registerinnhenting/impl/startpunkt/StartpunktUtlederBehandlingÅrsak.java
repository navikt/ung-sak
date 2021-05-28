package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef("BehandlingÅrsak")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("OMP")
class StartpunktUtlederBehandlingÅrsak implements EndringStartpunktUtleder {

    private BehandlingRepository behandlingRepository;

    StartpunktUtlederBehandlingÅrsak() {
        // For CDI
    }

    @Inject
    public StartpunktUtlederBehandlingÅrsak(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        if (behandling.getBehandlingÅrsaker().stream().anyMatch(it -> Objects.equals(it.getBehandlingÅrsakType(), BehandlingÅrsakType.G_REGULERING))) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.UDEFINERT;
    }


}
