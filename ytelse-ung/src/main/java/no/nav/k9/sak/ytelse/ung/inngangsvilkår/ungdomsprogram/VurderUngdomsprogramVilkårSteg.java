package no.nav.k9.sak.ytelse.ung.inngangsvilkår.ungdomsprogram;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR;

import java.util.List;
import java.util.NavigableSet;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

@BehandlingStegRef(value = VURDER_UNGDOMSPROGRAMVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class VurderUngdomsprogramVilkårSteg implements BehandlingSteg {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    public VurderUngdomsprogramVilkårSteg() {
    }

    @Inject
    public VurderUngdomsprogramVilkårSteg(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                          BehandlingRepository behandlingRepository,
                                          VilkårResultatRepository vilkårResultatRepository,
                                          UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET);
        var perioderTilVurdering = vilkårsPerioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.UNGDOMSPROGRAMVILKÅRET);
        var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId(), vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer());
        var builders = vurderPerioder(ungdomsprogramTidslinje, perioderTilVurdering, vilkårBuilder);
        builders.forEach(vilkårBuilder::leggTil);
        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultatBuilder.build());
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /**
     * Setter alle periodene til OPPFYLT
     *
     * @param ungdomsprogramTidslinje
     * @param perioderTilVurdering    Perioder som vurderes
     * @param vilkårBuilder           Vilkårbuilder
     * @return Vilkårperiodebuilders
     */
    private static List<VilkårPeriodeBuilder> vurderPerioder(LocalDateTimeline<Boolean> ungdomsprogramTidslinje, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårBuilder vilkårBuilder) {
        var builders = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering).combine(ungdomsprogramTidslinje, VurderUngdomsprogramVilkårSteg::settUtfall, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .toSegments()
            .stream()
            .map(p -> vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(p.getLocalDateInterval())).medUtfall(p.getValue()).medRegelInput("{ 'periode': '" + p.getLocalDateInterval() + "' }")).toList();
        return builders;
    }

    private static LocalDateSegment<Utfall> settUtfall(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return rhs == null ? new LocalDateSegment<>(di, Utfall.IKKE_OPPFYLT) : new LocalDateSegment<>(di, Utfall.OPPFYLT);
    }

}
