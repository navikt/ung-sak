package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.Regelsporing;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.List;

public record VurderingEtterAvklaringResultat(
    LocalDateTimeline<VilkårsvurderingResultatOgEvaluering> vurderinger,
    RegelInput regelInput
) {
    private static final VilkårJsonObjectMapper JSON = new VilkårJsonObjectMapper();

    public Regelsporing tilRegelsporing(RegelEvaluering evaluering) {
        return new Regelsporing(JSON.writeValueAsString(regelInput), JSON.writeValueAsString(evaluering));
    }

    public record VilkårsvurderingResultatOgEvaluering(
        VilkårsvurderingResultat resultat,
        RegelEvaluering evaluering
    ) {}

    public record RegelInput(
        List<VurdertPeriode> vurdertePerioder,
        List<AvklartPeriode> avklaringer,
        List<Periode> eksisterendeVilkårperioder
    ) {}

    public record VurdertPeriode(
        LocalDate fom,
        LocalDate tom,
        boolean erVilkårOppfylt
    ) {}

    public record AvklartPeriode(
        LocalDate fom,
        LocalDate tom,
        String ikkeOppfyltÅrsak
    ) {}

    public record RegelEvaluering(
        LocalDate fom,
        LocalDate tom,
        String utfall,
        String ikkeOppfyltÅrsak,
        boolean klippetMotVilkårsperiode
    ) {}
}
