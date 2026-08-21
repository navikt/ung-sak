package no.nav.ung.ytelse.aktivitetspenger.foreslåresultat;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
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
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
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
     * Behandlingen skal opphøres dersom det finnes en {@link VilkårsavklaringTjeneste} som gjelder for en av
     * behandlingens årsaker, hvis seneste avklaring under arbeid er av typen {@link Avklaringtype#OPPHØR},
     * og det finnes en avslått vilkårsperiode som overlapper avklaringens periode.
     */
    @Override
    protected boolean skalBehandlingResultatSettesTilOpphør(BehandlingReferanse ref, Vilkårene vilkårene) {
        // Avgrenser hvilke behandlingsårsaker vi leter etter opphør for
        var behandlingårsakerSomSkalKunneEndreBehandlingResultat = Set.of(
            BehandlingÅrsakType.ENDRET_BOSTED
        );

        Behandling behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var behandlingÅrsakerTyper = behandling.getBehandlingÅrsakerTyper()
                .stream().filter(behandlingårsakerSomSkalKunneEndreBehandlingResultat::contains)
                .toList();

        // Det er kun opphør dersom avklaringen faktisk gjelder en periode med avslått vilkår (overlapp).
        return behandlingÅrsakerTyper.stream()
            .flatMap(årsak -> VilkårsavklaringTjeneste.finnForÅrsak(alleVilkårsavklaringTjenester, årsak).stream())
            .flatMap(oppdaterer -> oppdaterer.hentSenesteAvklaringUnderArbeid(ref.getBehandlingId()).stream())
            .filter(avklaring -> Avklaringtype.OPPHØR.equals(avklaring.avklaringtype()))
            .anyMatch(avklaring -> harOverlappendeAvslåttVilkårsperiode(vilkårene, avklaring.periode()));
    }

    private boolean harOverlappendeAvslåttVilkårsperiode(Vilkårene vilkårene, DatoIntervallEntitet periode) {
        var vilkårTidslinjer = vilkårene.getVilkårTidslinjer(periode);
        return vilkårTidslinjer.values().stream().anyMatch(this::harAvslåtteVilkårsPerioder);
    }
}
