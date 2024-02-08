package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;

@ApplicationScoped
public class PleietrengendeRevurderingPerioderTjeneste {

    private BehandlingRepository behandlingRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste;

    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;

    PleietrengendeRevurderingPerioderTjeneste() {
        //
    }

    @Inject
    public PleietrengendeRevurderingPerioderTjeneste(BehandlingRepository behandlingRepository,
                                                     MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                                     ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                     EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
    }


    /** Finner tidslinje for endringer på pleietrengende med tilhørende behandlingsårsak
     *
     * @param referanse Behandlingsreferanser
     * @param vurderingsperioder Oppdaterte vurderingsperioder etter vurdering av sykdom
     * @return Tidslinje med årsaker
     */
    public LocalDateTimeline<Set<BehandlingÅrsakType>> utledBerørtePerioderPåPleietrengende(BehandlingReferanse referanse,
                                                                                            NavigableSet<DatoIntervallEntitet> vurderingsperioder) {
        LocalDateTimeline<Set<BehandlingÅrsakType>> utvidedePerioder = utledUtvidetPeriodeForSykdom(referanse, vurderingsperioder);
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


    private LocalDateTimeline<Set<BehandlingÅrsakType>> utledUtvidetPeriodeForSykdom(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> vurderingsperioder) {
        var forrigeVedtatteBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow()).getUuid();
        var vedtattSykdomGrunnlagBehandling = medisinskGrunnlagTjeneste.hentGrunnlagHvisEksisterer(forrigeVedtatteBehandling);
        var pleietrengende = referanse.getPleietrengendeAktørId();
        var utledetGrunnlag = medisinskGrunnlagTjeneste.utledGrunnlagMedManglendeOmsorgFjernet(referanse.getSaksnummer(), referanse.getBehandlingUuid(), referanse.getBehandlingId(), pleietrengende, vurderingsperioder);
        var diffPerioder = medisinskGrunnlagTjeneste.sammenlignGrunnlag(vedtattSykdomGrunnlagBehandling.map(MedisinskGrunnlag::getGrunnlagsdata), utledetGrunnlag).getDiffPerioder();
        return diffPerioder.map(s -> List.of(new LocalDateSegment<>(s.getLocalDateInterval(), Set.of(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON))));
    }

}
