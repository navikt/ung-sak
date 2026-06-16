package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(StartdatoGrunnlag.class)
@FagsakYtelseTypeRef
public class UngdomsytelseStartdatoGrunnlagBehandlingÅrsakUtleder implements BehandlingÅrsakUtleder {

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Set.of(BehandlingÅrsakType.NY_SØKT_PERIODE);
    }
}
