package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class BekreftetUttakTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    public BekreftetUttakTjeneste() {
    }

    @Inject
    public BekreftetUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                  @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurderingSomBlittAvslåttIBeregning(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var beregningsgrunnlagsvilkåret = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        return utledPerioderTilVurderingSomBlittAvslåttIBeregning(perioderTilVurdering, beregningsgrunnlagsvilkåret);
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderTilVurderingSomBlittAvslåttIBeregning(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                                                                                  Vilkår beregningsgrunnlagsvilkåret) {
        return beregningsgrunnlagsvilkåret.getPerioder()
            .stream()
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()) && perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
