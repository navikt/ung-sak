package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;

@ApplicationScoped
public class PleietrengendeRevurderingPerioderTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;

    PleietrengendeRevurderingPerioderTjeneste() {
        //
    }

    @Inject
    public PleietrengendeRevurderingPerioderTjeneste(BehandlingRepository behandlingRepository,
                                                     VilkårResultatRepository vilkårResultatRepository,
                                                     MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                                     ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                     EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
    }


    /**
     * Finner tidslinje for endringer på pleietrengende med tilhørende behandlingsårsak
     *
     * @param referanse         Behandlingsreferanser
     * @param definerendeVilkår Definerende vilkår for ytelsen
     * @return Tidslinje med årsaker
     */
    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledBerørtePerioderPåPleietrengende(BehandlingReferanse referanse,
                                                                                            Set<VilkårType> definerendeVilkår) {
        LocalDateTimeline<Set<BehandlingÅrsakType>> utvidedePerioder = utledUtvidetPeriodeForSykdom(referanse, definerendeVilkår);
        utvidedePerioder = utvidedePerioder.union(utledPerioderEndringIEtablertTilsyn(referanse), StandardCombinators::union);
        utvidedePerioder = utvidedePerioder.union(utledPerioderMedEndringIUnntakFraEtablertTilsyn(referanse), StandardCombinators::union);
        return utvidedePerioder;
    }

    private LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderMedEndringIUnntakFraEtablertTilsyn(BehandlingReferanse referanse) {
        return endringUnntakEtablertTilsynTjeneste.perioderMedEndringerSidenBehandling(referanse.getOriginalBehandlingId().orElse(null), referanse.getPleietrengendeAktørId())
            .map(s -> List.of(new LocalDateSegment<>(s.getLocalDateInterval(), Set.of(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON))));
    }

    private LocalDateTimeline<Set<BehandlingÅrsakType>> utledPerioderEndringIEtablertTilsyn(BehandlingReferanse referanse) {
        return etablertTilsynTjeneste.perioderMedEndringerFraForrigeBehandling(referanse)
            .map(s -> List.of(new LocalDateSegment<>(s.getLocalDateInterval(), Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON))));
    }


    private LocalDateTimeline<Set<BehandlingÅrsakType>> utledUtvidetPeriodeForSykdom(BehandlingReferanse referanse, Set<VilkårType> definerendeVilkår) {
        var forrigeVedtatteBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow()).getUuid();
        var vedtattSykdomGrunnlagBehandling = medisinskGrunnlagTjeneste.hentGrunnlagHvisEksisterer(forrigeVedtatteBehandling);
        var pleietrengende = referanse.getPleietrengendeAktørId();
        var vurderingsperioder = utledVurderingsperiode(vilkårResultatRepository.hent(referanse.getBehandlingId()), definerendeVilkår);
        var utledetGrunnlag = medisinskGrunnlagTjeneste.utledGrunnlagMedManglendeOmsorgFjernet(referanse.getSaksnummer(), referanse.getBehandlingUuid(), referanse.getBehandlingId(), pleietrengende, vurderingsperioder);
        var diffPerioder = medisinskGrunnlagTjeneste.sammenlignGrunnlag(vedtattSykdomGrunnlagBehandling.map(MedisinskGrunnlag::getGrunnlagsdata), utledetGrunnlag).getDiffPerioder();
        return diffPerioder.map(s -> List.of(new LocalDateSegment<>(s.getLocalDateInterval(), Set.of(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON))));
    }


    private NavigableSet<DatoIntervallEntitet> utledVurderingsperiode(Vilkårene vilkårene, Set<VilkårType> definerendeVilkår) {
        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();
        for (VilkårType vilkårType : definerendeVilkår) {
            var vilkår = vilkårene.getVilkår(vilkårType);
            if (vilkår.isPresent()) {
                var vilkårperioder = vilkår.get().getPerioder();
                var perioder = vilkårperioder.stream().map(VilkårPeriode::getPeriode).toList();
                tidslinje = tidslinje.combine(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
    }

}
