package no.nav.k9.sak.domene.registerinnhenting;

import jakarta.enterprise.inject.Alternative;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@Alternative
@GrunnlagRef(GrunnlagAggregatMock.class)
@FagsakYtelseTypeRef
class StartpunktUtlederMock implements EndringStartpunktUtleder {

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return StartpunktType.KONTROLLER_FAKTA;
    }
}
