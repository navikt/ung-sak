package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsvarselInnhold;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

/**
 * Vilkårsavklaringer for vilkårene i {@link VilkårsavklaringÅrsaker}.
 */
@ApplicationScoped
public class VilkårsavklaringTjeneste {

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    VilkårsavklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårsavklaringTjeneste(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }

    public void validerAvklartePerioderOverlapperEksisterendeVilkårsperioder(long behandlingId, VilkårType vilkårType,
                                                                          LocalDateTimeline<?> eksisterendeVilkårperioder, List<ÅpenPeriode> perioder) {
        validerStøttet(vilkårType);
        for (var periode : perioder) {
            boolean erÅpenPeriode = periode.getTom() == null || periode.getTom().equals(TIDENES_ENDE);
            if (erÅpenPeriode) {
                var fomTidslinje = new LocalDateTimeline<>(periode.getFom(), periode.getFom(), Boolean.TRUE);
                if (!fomTidslinje.intersects(eksisterendeVilkårperioder)) {
                    throw new IllegalArgumentException("Fom for åpen vurdert periode " + periode
                        + " overlapper ikke eksisterende vilkårperioder for " + vilkårType + " på behandlingId=" + behandlingId);
                }
            } else {
                var periodeTidslinje = new LocalDateTimeline<>(periode.getFom(), periode.getTom(), Boolean.TRUE);
                if (!periodeTidslinje.disjoint(eksisterendeVilkårperioder).isEmpty()) {
                    throw new IllegalArgumentException("Lukket vurdert periode " + periode
                        + " overlapper ikke i sin helhet med eksisterende vilkårperioder for " + vilkårType + " på behandlingId=" + behandlingId);
                }
            }
        }
    }

    public Map<VilkårsvarselInnhold, UUID> hentForeslåtteAvklaringerSomInnhold(long behandlingId, VilkårType vilkårType, VilkårsvarselInnholdMapper mapper) {
        return tilInnholdMap(mapper, hentForeslåtteAvklaringer(behandlingId, vilkårType));
    }

    /**
     * Etterlysningen brukeren allerede har fått peker på referansen til avklaringen. Endres noe som ikke vises for
     * bruker (typisk begrunnelsen), lagres avklaringen på nytt i et nytt grunnlag — da må referansen følge med,
     * ellers ville etterlysningen som beholdes pekt på en avklaring i et deaktivert grunnlag.
     */
    public Map<VilkårsvarselInnhold, UUID> lagreForeslåtteAvklaringer(long behandlingId, VilkårType vilkårType, Set<? extends ForeslåttVilkårsavklaring> nyeAvklaringer, VilkårsvarselInnholdMapper mapper) {
        var referanserPerVarselinnhold = hentForeslåtteAvklaringerSomInnhold(behandlingId, vilkårType, mapper);
        var nyeEntiteter = nyeAvklaringer.stream()
            .map(avklaring -> avklaring.tilEntitet(referanserPerVarselinnhold.getOrDefault(avklaring.innhold(), UUID.randomUUID())))
            .collect(Collectors.toSet());
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, vilkårType, nyeEntiteter);
        return tilInnholdMap(mapper, lagret);
    }

    /**
     * Ferdigstiller vilkårsavklaringene som er foreslått i behandlingen. Kalles ved iverksettelse av vedtaket.
     */
    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        støttedeVilkår().forEach(vilkårType -> vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandlingId, vilkårType));
    }

    public List<DatoIntervallEntitet> hentPerioderForForeslåtteAvklaringer(long behandlingId, VilkårType vilkårType) {
        return hentForeslåtteAvklaringer(behandlingId, vilkårType).stream().map(VilkårPeriodeAvklaring::getPeriode).toList();
    }

    // Hvis det er flere vilkårsavklaringer lagret samtidig velges den med senest fom.
    public Optional<Vilkårsavklaring> hentSenesteForeslåtteAvklaringForBehandling(long behandlingId, VilkårType vilkårType) {
        return hentForeslåtteAvklaringer(behandlingId, vilkårType).stream()
            .max(Comparator.comparing(VilkårPeriodeAvklaring::getVurdertTidspunkt)
                .thenComparing(avklaring -> avklaring.getPeriode().getFomDato()))
            .map(avklaring -> new Vilkårsavklaring(avklaring.getAvklaringtype(), avklaring.getPeriode(),
                AvklaringKilde.fraKode(vilkårType, avklaring.getKildeKode()), avklaring.getKildeFritekst()));
    }

    private Set<VilkårPeriodeAvklaring> hentForeslåtteAvklaringer(long behandlingId, VilkårType vilkårType) {
        validerStøttet(vilkårType);
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, vilkårType)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
    }

    private static Map<VilkårsvarselInnhold, UUID> tilInnholdMap(VilkårsvarselInnholdMapper mapper, Collection<? extends VilkårPeriodeAvklaring> avklaringer) {
        return avklaringer.stream()
            .collect(Collectors.toMap(mapper::tilVarselInnhold, VilkårPeriodeAvklaring::getReferanse));
    }

    private static List<VilkårType> støttedeVilkår() {
        return VilkårsavklaringÅrsaker.alle().keySet().stream().sorted().toList();
    }

    private static void validerStøttet(VilkårType vilkårType) {
        if (VilkårsavklaringÅrsaker.avklaringsårsakFor(vilkårType).isEmpty()) {
            throw new IllegalArgumentException("Vilkårsavklaring støttes ikke for " + vilkårType);
        }
    }
}
