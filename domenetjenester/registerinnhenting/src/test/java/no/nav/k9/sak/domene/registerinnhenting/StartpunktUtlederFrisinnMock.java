package no.nav.k9.sak.domene.registerinnhenting;

import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@Alternative
@FagsakYtelseTypeRef("FRISINN")
@GrunnlagRef("GrunnlagAggregatMock")
class StartpunktUtlederFrisinnMock implements StartpunktUtleder {

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return StartpunktType.BEREGNING;
    }
}
