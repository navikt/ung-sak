package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.ErEndringIRefusjonskravVurderer;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.DefaultVilkårUtleder;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.HarInntektsmeldingerRelevanteEndringerForPeriode;

@ApplicationScoped
public class RevurderingInntektsmeldingPeriodeTjeneste {

    private Instance<VilkårUtleder> vilkårUtledere;
    private HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode;
    private ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer;
    private BehandlingRepository behandlingRepository;

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;


    public RevurderingInntektsmeldingPeriodeTjeneste() {
    }

    @Inject
    public RevurderingInntektsmeldingPeriodeTjeneste(@Any Instance<VilkårUtleder> vilkårUtleder,
                                                     HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode,
                                                     ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer,
                                                     BehandlingRepository behandlingRepository,
                                                     SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.vilkårUtledere = vilkårUtleder;
        this.harInntektsmeldingerRelevanteEndringerForPeriode = harInntektsmeldingerRelevanteEndringerForPeriode;
        this.erEndringIRefusjonskravVurderer = erEndringIRefusjonskravVurderer;
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    /**
     * Utleder tidslinje for perioder der mottatte inntektsmeldinger i behandlingen skal føre til revurdering av periode
     * <p>
     * Utleder tidslinje med årsak til revurdering der det skilles mellom om inntektmeldingen fører til revurdering av et vilkår
     * eller til endring i tilkjent ytelse grunnet endring i refusjon.
     *
     * @param referanse                 Behandlingreferanse
     * @param inntektsmeldinger         inntektsmeldinger
     * @param mottatteInntektsmeldinger Mottatte dokumenter for inntektsmeldinger
     * @param perioder                  Aktuell vilkårsperiode
     * @return Tidslinje for perioder der inntektsmeldinger skal før til revurdering av periode, samt årsak til revurdering
     */
    @WithSpan
    public LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> utledTidslinjeForVurderingFraInntektsmelding(BehandlingReferanse referanse,
                                                                                                                Collection<Inntektsmelding> inntektsmeldinger,
                                                                                                                List<MottattDokument> mottatteInntektsmeldinger,
                                                                                                                Collection<DatoIntervallEntitet> perioder) {
        if (mottatteInntektsmeldinger.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        var utledeteVilkår = getVilkårUtleder(referanse).utledVilkår(referanse);
        LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> inntektsmeldingEndringer = LocalDateTimeline.empty();
        var originalBehandlingReferanse = finnOriginalBehandlingReferanse(referanse);
        for (var periode : perioder) {
            inntektsmeldingEndringer = finnTidslinjerForVilkårsperioderTilVurderingGrunnetInntektsmelding(referanse, inntektsmeldinger, mottatteInntektsmeldinger, periode, utledeteVilkår, inntektsmeldingEndringer);
            inntektsmeldingEndringer = inntektsmeldingEndringer.crossJoin(finnTidslinjeForRefusjonskravendringerIInntektsmelding(referanse, inntektsmeldinger, mottatteInntektsmeldinger, periode, originalBehandlingReferanse),
                StandardCombinators::union);
        }
        return inntektsmeldingEndringer;

    }

    private LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> finnTidslinjeForRefusjonskravendringerIInntektsmelding(BehandlingReferanse referanse,
                                                                                                                           Collection<Inntektsmelding> inntektsmeldinger,
                                                                                                                           List<MottattDokument> mottatteInntektsmeldinger,
                                                                                                                           DatoIntervallEntitet periode,
                                                                                                                           BehandlingReferanse originalBehandlingReferanse) {
        var alleSøknadsperioder = søknadsperiodeTjeneste.utledFullstendigPeriode(referanse.getBehandlingId());
        return erEndringIRefusjonskravVurderer.finnEndringstidslinjeForRefusjon(referanse, originalBehandlingReferanse, periode, inntektsmeldinger, mottatteInntektsmeldinger)
            .intersection(TidslinjeUtil.tilTidslinjeKomprimert(alleSøknadsperioder))
            .mapValue(it -> Set.of(InntektsmeldingRevurderingÅrsak.ENDRET_REFUSJONSKRAV));
    }

    private LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> finnTidslinjerForVilkårsperioderTilVurderingGrunnetInntektsmelding(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, List<MottattDokument> mottatteInntektsmeldinger, DatoIntervallEntitet periode, UtledeteVilkår utledeteVilkår, LocalDateTimeline<Set<InntektsmeldingRevurderingÅrsak>> inntektsmeldingEndringer) {
        for (var vilkår : utledeteVilkår.getAlleAvklarte()) {
            var inntektsmeldingerMedRelevanteEndringer = harInntektsmeldingerRelevanteEndringerForPeriode.finnInntektsmeldingerMedRelevanteEndringerForPeriode(inntektsmeldinger, referanse, periode, vilkår);
            var nyeRelevanteMottatteDokumenter = mottatteInntektsmeldinger.stream()
                .filter(im -> inntektsmeldingerMedRelevanteEndringer.stream().anyMatch(at -> Objects.equals(at.getJournalpostId(), im.getJournalpostId())))
                .filter(im -> im.getBehandlingId().equals(referanse.getBehandlingId()))
                .toList();

            if (!nyeRelevanteMottatteDokumenter.isEmpty()) {
                inntektsmeldingEndringer = inntektsmeldingEndringer.crossJoin(new LocalDateTimeline<>(periode.toLocalDateInterval(),
                        Set.of(InntektsmeldingRevurderingÅrsak.REVURDERER_VILKÅR)),
                    StandardCombinators::union);
            }
        }
        return inntektsmeldingEndringer;
    }

    private BehandlingReferanse finnOriginalBehandlingReferanse(BehandlingReferanse referanse) {
        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());
        return BehandlingReferanse.fra(originalBehandling);
    }

    private VilkårUtleder getVilkårUtleder(BehandlingReferanse referanse) {
        return FagsakYtelseTypeRef.Lookup.find(vilkårUtledere, referanse.getFagsakYtelseType()).orElse(new DefaultVilkårUtleder());
    }


}
