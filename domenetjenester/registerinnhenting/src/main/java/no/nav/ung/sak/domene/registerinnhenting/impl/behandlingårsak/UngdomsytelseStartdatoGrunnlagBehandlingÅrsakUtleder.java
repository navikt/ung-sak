package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(UngdomsytelseStartdatoGrunnlag.class)
@FagsakYtelseTypeRef
public class UngdomsytelseStartdatoGrunnlagBehandlingÅrsakUtleder implements BehandlingÅrsakUtleder {

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Set.of(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }
}
