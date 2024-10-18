package no.nav.k9.sak.ytelse.ung.periode;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.ung.inngangsvilkår.InngangsvilkårUtleder;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseVilkårsperioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private InngangsvilkårUtleder inngangsvilkårUtleder;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    UngdomsytelseVilkårsperioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVilkårsperioderTilVurderingTjeneste(
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) InngangsvilkårUtleder inngangsvilkårUtleder, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId);
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.UNGDOMSPROGRAMVILKÅRET);
    }

    private TreeSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        TreeSet<DatoIntervallEntitet> periode = ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(this::bestemPeriode)
            .collect(Collectors.toCollection(TreeSet::new));


        return periode;
    }

    private DatoIntervallEntitet bestemPeriode(UngdomsprogramPeriode it) {
        DatoIntervallEntitet periode = it.getPeriode();
        // TOM dato fra register kan være null som mapper til tidenes ende. Men vi lar likevel vilkåret ha en enkel
        // maksgrense foreløpig
        if (periode.getTomDato().equals(AbstractLocalDateInterval.TIDENES_ENDE)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(
                periode.getFomDato(), periode.getFomDato().plus(PeriodeKonstanter.MAKS_PERIODE));
        }

        return periode;
    }
}
