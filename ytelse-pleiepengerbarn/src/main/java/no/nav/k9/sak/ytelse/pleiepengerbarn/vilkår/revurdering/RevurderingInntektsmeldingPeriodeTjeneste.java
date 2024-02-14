package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.DefaultVilkårUtleder;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.HarInntektsmeldingerRelevanteEndringerForPeriode;

@ApplicationScoped
public class RevurderingInntektsmeldingPeriodeTjeneste {

    private Instance<VilkårUtleder> vilkårUtledere;
    private HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode;
    private ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer;


    public RevurderingInntektsmeldingPeriodeTjeneste() {
    }

    @Inject
    public RevurderingInntektsmeldingPeriodeTjeneste(@Any Instance<VilkårUtleder> vilkårUtleder,
                                                     HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode,
                                                     ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer) {
        this.vilkårUtledere = vilkårUtleder;
        this.harInntektsmeldingerRelevanteEndringerForPeriode = harInntektsmeldingerRelevanteEndringerForPeriode;
        this.erEndringIRefusjonskravVurderer = erEndringIRefusjonskravVurderer;
    }

    public LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse referanse,
                                                                                                                Collection<Inntektsmelding> inntektsmeldinger,
                                                                                                                List<MottattDokument> mottatteInntektsmeldinger,
                                                                                                                Collection<DatoIntervallEntitet> perioder) {
        var utledeteVilkår = getVilkårUtleder(referanse).utledVilkår(referanse);
        LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> inntektsmeldingEndringer = LocalDateTimeline.empty();
        for (var periode : perioder) {
            for (var vilkår : utledeteVilkår.getAlleAvklarte()) {
                var inntektsmeldingerMedRelevanteEndringer = harInntektsmeldingerRelevanteEndringerForPeriode.finnInntektsmeldingerMedRelevanteEndringerForPeriode(inntektsmeldinger, referanse, periode, vilkår);
                var nyeRelevanteMottatteDokumenter = mottatteInntektsmeldinger.stream()
                    .filter(im -> inntektsmeldingerMedRelevanteEndringer.stream().anyMatch(at -> Objects.equals(at.getJournalpostId(), im.getJournalpostId())))
                    .toList();

                if (!nyeRelevanteMottatteDokumenter.isEmpty()) {
                    inntektsmeldingEndringer.crossJoin(new LocalDateTimeline<>(periode.toLocalDateInterval(),
                            Set.of(InntektsmeldingRevurderingÅrsak.REVURDERER_VILKÅR)),
                        StandardCombinators::union);
                }
            }
            inntektsmeldingEndringer.crossJoin(erEndringIRefusjonskravVurderer.finnEndringstidslinjeForRefusjon(referanse, periode).mapValue(it -> Set.of(InntektsmeldingRevurderingÅrsak.ENDRET_REFUSJONSKRAV)),
                StandardCombinators::union);
        }
        return inntektsmeldingEndringer;

    }

    private VilkårUtleder getVilkårUtleder(BehandlingReferanse referanse) {
        return FagsakYtelseTypeRef.Lookup.find(vilkårUtledere, referanse.getFagsakYtelseType()).orElse(new DefaultVilkårUtleder());
    }


}
