package no.nav.ung.ytelse.aktivitetspenger.foreslåresultat;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class ForeslåBehandlingsresultatAktivitetspengerTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private BehandlingRepository behandlingRepository;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private Instance<VilkårsavklaringTjeneste> alleVilkårsavklaringTjenester;

    ForeslåBehandlingsresultatAktivitetspengerTjeneste() {
        // for proxy
    }

    @Inject
    public ForeslåBehandlingsresultatAktivitetspengerTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                              @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                              @Any Instance<VilkårsavklaringTjeneste> alleVilkårsavklaringTjenester) {
        super(repositoryProvider);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.alleVilkårsavklaringTjenester = alleVilkårsavklaringTjenester;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (VilkårType vilkårType : definerendeVilkår) {
            timeline = timeline.combine(
                TidslinjeUtil.tilTidslinje(vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, vilkårType)),
                StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN
            );
        }
        if (timeline.isEmpty()) {
            return behandling.getFagsak().getPeriode();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate());
    }

    /**
     * Behandlingen skal opphøres dersom seneste avklaring for et vilkår i behandlingen er av typen {@link Avklaringtype#OPPHØR},
     * og det samme vilkåret er avslått i avklaringens periode. Avkortede perioder regnes ikke som avslag.
     */
    @Override
    protected boolean skalBehandlingResultatSettesTilOpphør(BehandlingReferanse ref, Vilkårene vilkårene) {
        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var behandlingÅrsaker = behandling.getBehandlingÅrsakerTyper();

        return VilkårsavklaringÅrsaker.alle().entrySet().stream()
            .filter(entry -> behandlingÅrsaker.contains(entry.getValue()))
            .anyMatch(entry -> harOpphørMedAvslag(ref.getBehandlingId(), vilkårene, entry.getKey(), entry.getValue()));
    }

    private boolean harOpphørMedAvslag(Long behandlingId, Vilkårene vilkårene, VilkårType vilkårType, BehandlingÅrsakType årsak) {
        return VilkårsavklaringTjeneste.finnForÅrsak(alleVilkårsavklaringTjenester, årsak).stream()
            .flatMap(tjeneste -> tjeneste.hentSenesteAvklaringForBehandling(behandlingId).stream())
            .filter(avklaring -> Avklaringtype.OPPHØR.equals(avklaring.avklaringtype()))
            .anyMatch(avklaring -> harAvslagIPeriode(vilkårene, vilkårType, avklaring.periode()));
    }

    private static boolean harAvslagIPeriode(Vilkårene vilkårene, VilkårType vilkårType, DatoIntervallEntitet periode) {
        return !vilkårene.getVilkårTimeline(vilkårType, periode.getFomDato(), periode.getTomDato())
            .filterValue(vp -> vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT
                && vp.getAvslagsårsak() != null
                && vp.getAvslagsårsak() != Avslagsårsak.AVKORTET)
            .isEmpty();
    }
}
