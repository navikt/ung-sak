package no.nav.k9.sak.ytelse.frisinn.registerinnhenting;

import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;

@Alternative
@FagsakYtelseTypeRef("FRISINN")
@BehandlingTypeRef
@GrunnlagRef("GrunnlagAggregatMock")
class StartpunktUtlederFrisinnMock implements StartpunktUtleder {

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return StartpunktType.BEREGNING;
    }
}
