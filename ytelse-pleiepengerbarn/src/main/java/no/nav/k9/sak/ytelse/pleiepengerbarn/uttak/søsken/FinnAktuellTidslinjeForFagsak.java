package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;

@Dependent
public class FinnAktuellTidslinjeForFagsak {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final BehandlingRepository behandlingRepository;
    private final UttakTjeneste uttakTjeneste;

    @Inject
    public FinnAktuellTidslinjeForFagsak(VilkårResultatRepository vilkårResultatRepository,
                                         BehandlingRepository behandlingRepository, UttakTjeneste uttakTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    LocalDateTimeline<Boolean> finnTidslinje(Fagsak fagsak) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Forventer å finne minst en ytelsesbehandling for fagsak med id " + fagsak.getId()));
        var uttakstidslinje = finnUttakstidslinjeDersomRelevant(behandling);
        var vilkårtidslinje = finnVilkårTidslinje(behandling);
        return uttakstidslinje.map(vilkårtidslinje::intersection)
            .orElse(vilkårtidslinje);
    }

    private LocalDateTimeline<Boolean> finnVilkårTidslinje(Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        return vilkårene.stream()
            .flatMap(v -> v.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream())
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getUtfall().equals(Utfall.IKKE_VURDERT) || p.getUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), Boolean.TRUE))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private Optional<LocalDateTimeline<Boolean>> finnUttakstidslinjeDersomRelevant(Behandling behandling) {
        if (behandling.erAvsluttet()) {
            var uttaksplan = uttakTjeneste.hentUttaksplan(behandling.getUuid(), true);
            var uttakstidslinje = uttaksplan.getPerioder()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getUttaksgrad().compareTo(BigDecimal.ZERO) > 0)
                .map(Map.Entry::getKey)
                .map(p -> new LocalDateTimeline<>(p.getFom(), p.getTom(), Boolean.TRUE))
                .reduce(LocalDateTimeline::crossJoin)
                .orElse(LocalDateTimeline.empty());
            return Optional.of(uttakstidslinje);
        }
        return Optional.empty();
    }

}
