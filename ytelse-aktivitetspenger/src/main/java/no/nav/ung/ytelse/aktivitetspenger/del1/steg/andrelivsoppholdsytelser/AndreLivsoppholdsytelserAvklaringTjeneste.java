package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

@ApplicationScoped
@BehandlingÅrsakTypeRef(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE)
public class AndreLivsoppholdsytelserAvklaringTjeneste implements VilkårsavklaringTjeneste {

    private static final VilkårType VILKÅR_TYPE = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    public AndreLivsoppholdsytelserAvklaringTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AndreLivsoppholdsytelserAvklaringTjeneste(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                     InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                     VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

     public void validerAvklartePerioderOverlapperEksisterendeVilkårsperioder(long behandlingId, List<ÅpenPeriode> perioder) {
        var eksisterendeVilkårperioder = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .map(vilkårene -> vilkårene.getVilkårTimeline(VILKÅR_TYPE))
            .orElseThrow(() -> new IllegalArgumentException("Fant ingen vilkårsperioder for " + VILKÅR_TYPE + " på behandlingId=" + behandlingId));

        for (var periode : perioder) {
            boolean erÅpenPeriode = periode.getTom() == null || periode.getTom().equals(TIDENES_ENDE);
            if (erÅpenPeriode) {
                var fomTidslinje = new LocalDateTimeline<>(periode.getFom(), periode.getFom(), Boolean.TRUE);
                if (!fomTidslinje.intersects(eksisterendeVilkårperioder)) {
                    throw new IllegalArgumentException("Fom for åpen vurdert periode " + periode
                        + " overlapper ikke eksisterende vilkårperioder for " + VILKÅR_TYPE + " på behandlingId=" + behandlingId);
                }
            } else {
                var periodeTidslinje = new LocalDateTimeline<>(periode.getFom(), periode.getTom(), Boolean.TRUE);
                if (!periodeTidslinje.disjoint(eksisterendeVilkårperioder).isEmpty()) {
                    throw new IllegalArgumentException("Lukket vurdert periode " + periode
                        + " overlapper ikke i sin helhet med eksisterende vilkårperioder for " + VILKÅR_TYPE + " på behandlingId=" + behandlingId);
                }
            }
        }
    }

    public Map<VilkårsvarselInnhold, UUID> hentForeslåtteAvklaringerSomInnhold(long behandlingId) {
        return tilInnholdMap(hentForeslåtteAvklaringer(behandlingId));
    }

    public Map<VilkårsvarselInnhold, UUID> lagreForeslåtteAvklaringer(long behandlingId, Set<AndreLivsoppholdsytelserAvklaring> nyeAvklaringer) {
        var referanserPerVarselinnhold = hentForeslåtteAvklaringerSomInnhold(behandlingId);
        var nyeEntiteter = nyeAvklaringer.stream()
            .map(avklaring -> AndreLivsoppholdsytelserAvklaringDataMapper.mapTilVilkårPeriodeAvklaring(avklaring, referanseFor(avklaring, referanserPerVarselinnhold)))
            .collect(Collectors.toSet());
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VILKÅR_TYPE, nyeEntiteter);
        return tilInnholdMap(lagret);
    }

    /**
     * Etterlysningen brukeren allerede har fått peker på referansen til avklaringen. Endres noe som ikke vises for
     * bruker (typisk begrunnelsen), lagres avklaringen på nytt i et nytt grunnlag — da må referansen følge med,
     * ellers ville etterlysningen som beholdes pekt på en avklaring i et deaktivert grunnlag.
     */
    private static UUID referanseFor(AndreLivsoppholdsytelserAvklaring avklaring, Map<VilkårsvarselInnhold, UUID> referanserPerVarselinnholdForEksisterendeAvklaringer) {
        return referanserPerVarselinnholdForEksisterendeAvklaringer.getOrDefault(avklaring.innhold(), UUID.randomUUID());
    }

    private static Map<VilkårsvarselInnhold, UUID> tilInnholdMap(Collection<VilkårPeriodeAvklaring> avklaringer) {
        return avklaringer.stream()
            .collect(Collectors.toMap(AndreLivsoppholdsytelserAvklaringDataMapper::mapTilAvklaringInnhold, VilkårPeriodeAvklaring::getReferanse));
    }

    private Set<VilkårPeriodeAvklaring> hentForeslåtteAvklaringer(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VILKÅR_TYPE)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
    }

    @Override
    public void ferdigstillForeslåtteAvklaringer(long behandlingId) {
        vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandlingId, VILKÅR_TYPE);
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForForeslåtteAvklaringer(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = hentForeslåtteAvklaringer(behandlingId).stream()
            .map(VilkårPeriodeAvklaring::getPeriode)
            .toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VILKÅR_TYPE, perioderTidligereVurdertEtterAvklaring);
    }

    @Override
    public Optional<Vilkårsavklaring> hentSenesteForeslåtteAvklaringForBehandling(long behandlingId) {
        return hentForeslåtteAvklaringer(behandlingId).stream()
            .max(Comparator.comparing(VilkårPeriodeAvklaring::getVurdertTidspunkt)
                .thenComparing(avklaring -> avklaring.getPeriode().getFomDato()))
            .map(avklaring -> new Vilkårsavklaring(
                avklaring.getAvklaringtype(),
                avklaring.getPeriode(),
                AndreLivsoppholdsytelserAvklaringKildeType.fraKode(avklaring.getKildeKode()),
                avklaring.getKildeFritekst()));
    }
}
