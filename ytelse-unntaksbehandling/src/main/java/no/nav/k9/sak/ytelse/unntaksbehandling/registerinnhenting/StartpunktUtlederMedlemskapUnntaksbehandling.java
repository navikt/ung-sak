package no.nav.k9.sak.ytelse.unntaksbehandling.registerinnhenting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.medlem.MedlemEndringIdentifiserer;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-010")
//@GrunnlagRef("MedlemskapAggregat")
class StartpunktUtlederMedlemskapUnntaksbehandling implements StartpunktUtleder {

    private MedlemskapRepository medlemskapRepository;
    private MedlemEndringIdentifiserer endringIdentifiserer;

    @Inject
    StartpunktUtlederMedlemskapUnntaksbehandling(MedlemskapRepository medlemskapRepository, MedlemEndringIdentifiserer endringIdentifiserer) {
        this.medlemskapRepository = medlemskapRepository;
        this.endringIdentifiserer = endringIdentifiserer;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.KONTROLLER_FAKTA, "medlemskap", grunnlagId1, grunnlagId2);
        return StartpunktType.KONTROLLER_FAKTA;
    }
}
