package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.medlem.MedlemEndringIdentifiserer;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef
@GrunnlagRef("MedlemskapAggregat")
class StartpunktUtlederMedlemskap implements StartpunktUtleder {

    private MedlemskapRepository medlemskapRepository;
    private MedlemEndringIdentifiserer endringIdentifiserer;

    @Inject
    StartpunktUtlederMedlemskap(MedlemskapRepository medlemskapRepository, MedlemEndringIdentifiserer endringIdentifiserer) {
        this.medlemskapRepository = medlemskapRepository;
        this.endringIdentifiserer = endringIdentifiserer;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        final MedlemskapAggregat grunnlag1 = medlemskapRepository.hentMedlemskapPåId((Long)grunnlagId1);
        final MedlemskapAggregat grunnlag2 = medlemskapRepository.hentMedlemskapPåId((Long)grunnlagId2);

        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        final boolean erEndretFørSkjæringstidspunkt = endringIdentifiserer.erEndretFørSkjæringstidspunkt(grunnlag1, grunnlag2, skjæringstidspunkt);
        if (erEndretFørSkjæringstidspunkt) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "medlemskap", grunnlagId1, grunnlagId2);
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        }
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "medlemskap", grunnlagId1, grunnlagId2);
        return StartpunktType.UTTAKSVILKÅR;
    }
}
