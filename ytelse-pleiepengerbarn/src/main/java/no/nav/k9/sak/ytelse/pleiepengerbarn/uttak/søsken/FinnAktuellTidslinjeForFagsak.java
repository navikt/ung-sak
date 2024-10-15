package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
public class FinnAktuellTidslinjeForFagsak {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FinnAktuellTidslinjeForFagsak(VilkårResultatRepository vilkårResultatRepository,
                                         BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    LocalDateTimeline<Boolean> finnTidslinje(Fagsak fagsak) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Forventer å finne minst en ytelsesbehandling for fagsak med id " + fagsak.getId()));
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

}
