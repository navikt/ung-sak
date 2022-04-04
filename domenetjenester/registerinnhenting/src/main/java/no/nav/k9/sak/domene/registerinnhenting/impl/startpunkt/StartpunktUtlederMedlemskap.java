package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.medlem.MedlemEndringIdentifiserer;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@GrunnlagRef("MedlemskapAggregat")
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
class StartpunktUtlederMedlemskap implements EndringStartpunktUtleder {

    private MedlemskapRepository medlemskapRepository;
    private MedlemEndringIdentifiserer endringIdentifiserer = new MedlemEndringIdentifiserer();
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    @Inject
    StartpunktUtlederMedlemskap(MedlemskapRepository medlemskapRepository,
                                @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.medlemskapRepository = medlemskapRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        final MedlemskapAggregat grunnlag1 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId1);
        final MedlemskapAggregat grunnlag2 = medlemskapRepository.hentMedlemskapPåId((Long) grunnlagId2);

        if (Objects.equals(ref.getFagsakYtelseType(), FRISINN)) {
            LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
            final boolean erEndretFørSkjæringstidspunkt = endringIdentifiserer.erEndretFørSkjæringstidspunkt(grunnlag1, grunnlag2, skjæringstidspunkt);
            if (erEndretFørSkjæringstidspunkt) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "medlemskap", grunnlagId1, grunnlagId2);
                return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
            }
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "medlemskap", grunnlagId1, grunnlagId2);
            return StartpunktType.UTTAKSVILKÅR;
        } else {
            var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(this.perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType());

            var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET);

            if (perioderTilVurdering.isEmpty()) {
                return StartpunktType.UDEFINERT;
            }

            var førsteDatoTilVurdering = perioderTilVurdering.stream()
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElseThrow();
            var sisteDatoTilVurdering = perioderTilVurdering.stream()
                .map(DatoIntervallEntitet::getFomDato)
                .max(LocalDate::compareTo)
                .orElseThrow();

            var erEndretIPerioden = endringIdentifiserer.erEndretIPerioden(grunnlag1, grunnlag2, DatoIntervallEntitet.fraOgMedTilOgMed(førsteDatoTilVurdering.minusWeeks(4), sisteDatoTilVurdering));

            if (erEndretIPerioden) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "medlemskap", grunnlagId1, grunnlagId2);
                return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
            }
            return StartpunktType.UDEFINERT;
        }
    }
}
