package no.nav.ung.ytelse.aktivitetspenger.foreslåresultat;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
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
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.SamletVilkårsutfallTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class ForeslåBehandlingsresultatAktivitetspengerTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private BehandlingRepository behandlingRepository;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private VilkårsavklaringTjeneste vilkårsavklaringTjeneste;

    ForeslåBehandlingsresultatAktivitetspengerTjeneste() {
        // for proxy
    }

    @Inject
    public ForeslåBehandlingsresultatAktivitetspengerTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                              @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                              VilkårsavklaringTjeneste vilkårsavklaringTjeneste) {
        super(repositoryProvider);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårsavklaringTjeneste = vilkårsavklaringTjeneste;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var timeline = finnTidslinjeTilVurdering(behandlingId);
        if (timeline.isEmpty()) {
            return behandling.getFagsak().getPeriode();
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(timeline.getMinLocalDate(), timeline.getMaxLocalDate());
    }

    private LocalDateTimeline<Boolean> finnTidslinjeTilVurdering(Long behandlingId) {
        var definerendeVilkår = vilkårsPerioderTilVurderingTjeneste.definerendeVilkår();
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (VilkårType vilkårType : definerendeVilkår) {
            timeline = timeline.combine(
                TidslinjeUtil.tilTidslinje(vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, vilkårType)),
                StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN
            );
        }
        return timeline;
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
            .anyMatch(entry -> harOpphørMedAvslag(ref.getBehandlingId(), vilkårene, entry.getKey()));
    }

    @Override
    protected boolean skalBehandlingResultatSettesTilAvslått(BehandlingReferanse ref, Vilkårene vilkårene) {
        var behandlingId = ref.getBehandlingId();
        if (harKunAvslåttVilkårutfallIVurdertePerioder(vilkårene, behandlingId)) {
            return true;
        }
        return skalAvslåsBasertPåAndreForhold(ref);
    }

    private boolean harKunAvslåttVilkårutfallIVurdertePerioder(Vilkårene vilkårene, Long behandlingId) {
        LocalDateTimeline<Boolean> tilVurdering = finnTidslinjeTilVurdering(behandlingId);
        if (tilVurdering.isEmpty()) {
            // ingen perioder til vurdering
            return false;
        }
        DatoIntervallEntitet maksPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(tilVurdering.getMinLocalDate(), tilVurdering.getMaxLocalDate());
        LocalDateTimeline<VilkårUtfallSamlet> samletVilkårutfall = SamletVilkårsutfallTjeneste.samleVilkårUtfall(vilkårene, maksPeriode);
        var vurderteUtfall = samletVilkårutfall.intersection(tilVurdering);
        return !vurderteUtfall.isEmpty() && vurderteUtfall.stream()
            .allMatch(it -> it.getValue().getSamletUtfall() == Utfall.IKKE_OPPFYLT);
    }

    private boolean harOpphørMedAvslag(Long behandlingId, Vilkårene vilkårene, VilkårType vilkårType) {
        return vilkårsavklaringTjeneste.hentSenesteForeslåtteAvklaringForBehandling(behandlingId, vilkårType).stream()
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
