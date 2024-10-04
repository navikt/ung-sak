package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søskensak;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;

@Dependent
public class PleietrengendeUttaksprioritetMotAndrePleietrengende {

    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;
    private KalkulusTjeneste kalkulusTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private boolean søskensakPrioriteringEnabled;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public PleietrengendeUttaksprioritetMotAndrePleietrengende(FagsakRepository fagsakRepository,
                                                               BehandlingRepository behandlingRepository,
                                                               KalkulusTjeneste kalkulusTjeneste,
                                                               BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                               @KonfigVerdi(value = "SOKSENSAK_PRIORITERING_ENABLED", defaultVerdi = "false") boolean søskensakPrioriteringEnabled,
                                                               VilkårResultatRepository vilkårResultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.søskensakPrioriteringEnabled = søskensakPrioriteringEnabled;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public LocalDateTimeline<List<Uttakprioritet>> vurderUttakprioritetEgneSaker(Long fagsakId, boolean brukUbesluttedeData) {
        if (!søskensakPrioriteringEnabled) {
            return LocalDateTimeline.empty();
        }

        Fagsak aktuellFagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        final List<Fagsak> fagsaker = utledFagsakerRelevantForKravprioEgneSaker(aktuellFagsak);

        return vurderUttakprioritetFraFagsakerForAndrePleietrengende(fagsakId, brukUbesluttedeData, fagsaker);
    }

    private LocalDateTimeline<List<Uttakprioritet>> vurderUttakprioritetFraFagsakerForAndrePleietrengende(Long fagsakId, boolean brukUbesluttedeData, List<Fagsak> fagsaker) {
        LocalDateTimeline<List<Uttakprioritet>> kravprioritetstidslinje = LocalDateTimeline.empty();
        for (Fagsak fagsak : fagsaker) {
            final boolean brukAvsluttetBehandling = !brukUbesluttedeData && !fagsak.getId().equals(fagsakId);


            final LocalDateTimeline<Uttakprioritet> fagsakTidslinje = finnBgTidslinjeForFagsak(fagsak, brukAvsluttetBehandling);
            kravprioritetstidslinje = kravprioritetstidslinje.union(fagsakTidslinje, sortertMedStørsteBgFørst());
        }

        return kravprioritetstidslinje.compress();
    }


    private List<Fagsak> utledFagsakerRelevantForKravprioEgneSaker(Fagsak aktuellFagsak) {
        if (Objects.equals(FagsakYtelseType.OPPLÆRINGSPENGER, aktuellFagsak.getYtelseType())) {
            return List.of(aktuellFagsak);
        }
        return fagsakRepository.finnFagsakRelatertTil(aktuellFagsak.getYtelseType(), aktuellFagsak.getBrukerAktørId(), null, null, null, null).stream()
            .toList();
    }


    private LocalDateTimeline<Uttakprioritet> finnBgTidslinjeForFagsak(Fagsak fagsak, boolean brukAvsluttetBehandling) {
        LocalDateTimeline<Uttakprioritet> fagsakTidslinje = LocalDateTimeline.empty();
        final Optional<Behandling> behandlingOpt;
        if (brukAvsluttetBehandling) {
            behandlingOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        } else {
            behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        }
        if (behandlingOpt.isEmpty()) {
            return fagsakTidslinje;
        }

        var bgVilkårTidslinje = vilkårResultatRepository.hent(behandlingOpt.get().getId())
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> Utfall.OPPFYLT.equals(p.getUtfall()))
            .map(p -> new LocalDateTimeline<>(p.getFom(), p.getTom(), Boolean.TRUE))
            .reduce(LocalDateTimeline.empty(), LocalDateTimeline::crossJoin);

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingOpt.get().getId());

        var alleReferanser = beregningsgrunnlagPerioderGrunnlag.stream()
            .flatMap(gr -> gr.getGrunnlagPerioder().stream())
            .map(p -> new BgRef(p.getEksternReferanse(), p.getSkjæringstidspunkt()))
            .collect(Collectors.toSet());

        var alleBeregningsgrunnlag = kalkulusTjeneste.hentGrunnlag(BehandlingReferanse.fra(behandlingOpt.get()), alleReferanser);

        var bgSegmenter = alleBeregningsgrunnlag.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlag().stream())
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder()
                .stream()
                .filter(p -> p.getPeriode().getFomDato().isEqual(bg.getSkjæringstidspunkt()) ||
                    !p.getPeriode().getTomDato().isEqual(TIDENES_ENDE)))
            .map(p -> new LocalDateSegment<>(
                p.getBeregningsgrunnlagPeriodeFom(),
                p.getBeregningsgrunnlagPeriodeTom(),
                new Uttakprioritet(behandlingOpt.get(), p.getBruttoPrÅr(), p.getBeregningsgrunnlag().getSkjæringstidspunkt())))
            .toList();
        return new LocalDateTimeline<>(bgSegmenter).intersection(bgVilkårTidslinje);
    }

    private LocalDateSegmentCombinator<List<Uttakprioritet>, Uttakprioritet, List<Uttakprioritet>> sortertMedStørsteBgFørst() {
        return (datoInterval, datoSegment, datoSegment2) -> {

            if (datoSegment == null) {
                return new LocalDateSegment<>(datoInterval, List.of(datoSegment2.getValue()));
            }
            if (datoSegment2 == null) {
                return new LocalDateSegment<>(datoInterval, datoSegment.getValue());
            }
            final List<Uttakprioritet> liste = new ArrayList<>(datoSegment.getValue());
            liste.add(datoSegment2.getValue());
            Collections.sort(liste);

            return new LocalDateSegment<>(datoInterval, liste);
        };
    }

    public static final class Uttakprioritet implements Comparable<Uttakprioritet> {
        /**
         * Gir siste gjeldende behandling der kravet inngår.
         * <p>
         * Dette er den åpne behandlingen for søker, og siste besluttede
         * behandling for andre søkere.
         */
        private final Behandling aktuellBehandling;
        private final BigDecimal bruttoBeregningsgrunnlag;
        private final LocalDate skjæringstidspunkt;

        public Uttakprioritet(Behandling aktuellBehandling, BigDecimal bruttoBeregningsgrunnlag, LocalDate skjæringstidspunkt) {
            this.aktuellBehandling = aktuellBehandling;
            this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
            this.skjæringstidspunkt = skjæringstidspunkt;
        }

        public UUID getAktuellBehandlingUuid() {
            return aktuellBehandling.getUuid();
        }

        public BigDecimal getBruttoBeregningsgrunnlag() {
            return bruttoBeregningsgrunnlag;
        }


        public int compareTo(Uttakprioritet other) {
            final int result = other.bruttoBeregningsgrunnlag.compareTo(bruttoBeregningsgrunnlag); // Største først
            if (result == 0) {
                return skjæringstidspunkt.compareTo(other.skjæringstidspunkt);
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Uttakprioritet that)) return false;
            return Objects.equals(aktuellBehandling, that.aktuellBehandling) && Objects.equals(bruttoBeregningsgrunnlag, that.bruttoBeregningsgrunnlag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aktuellBehandling, bruttoBeregningsgrunnlag);
        }
    }

}
