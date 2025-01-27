package no.nav.ung.sak.inngangsvilkår.ungdomsprogram;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR;

import java.util.List;
import java.util.NavigableSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

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
        var ungdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        var builders = vurderPerioder(ungdomsprogramTidslinje, perioderTilVurdering, vilkårBuilder);
        builders.forEach(vilkårBuilder::leggTil);
        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), resultatBuilder.build());
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /**
     * Setter perioder der bruker deltar i ungdomsprogrammet til oppfylt, andre perioder settes til avslått
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
