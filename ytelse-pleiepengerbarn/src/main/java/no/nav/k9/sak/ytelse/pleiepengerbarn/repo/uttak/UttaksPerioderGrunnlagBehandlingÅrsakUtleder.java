package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;

@ApplicationScoped
@GrunnlagRef("UttakPerioderGrunnlag")
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class UttaksPerioderGrunnlagBehandlingÅrsakUtleder implements BehandlingÅrsakUtleder {

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }
}
