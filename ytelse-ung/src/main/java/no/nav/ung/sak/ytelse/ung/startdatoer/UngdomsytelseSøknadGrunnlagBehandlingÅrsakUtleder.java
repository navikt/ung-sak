package no.nav.ung.sak.ytelse.ung.startdatoer;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;

@ApplicationScoped
@GrunnlagRef(UngdomsytelseSøknadGrunnlag.class)
@FagsakYtelseTypeRef
public class UngdomsytelseSøknadGrunnlagBehandlingÅrsakUtleder implements BehandlingÅrsakUtleder {

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }
}
