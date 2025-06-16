package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.inject.Alternative;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

@Alternative
@GrunnlagRef(GrunnlagAggregatMock.class)
@FagsakYtelseTypeRef
class StartpunktUtlederMock implements EndringStartpunktUtleder {

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return StartpunktType.INIT_PERIODER;
    }
}
