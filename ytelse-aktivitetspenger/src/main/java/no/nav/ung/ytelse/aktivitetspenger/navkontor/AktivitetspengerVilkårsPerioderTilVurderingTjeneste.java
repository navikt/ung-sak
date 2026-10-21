package no.nav.ung.ytelse.aktivitetspenger.navkontor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.IngenVurdering;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.KantIKantVurderer;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.BehandlingstriggerPerioderTilVurderingTjeneste;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.UtledeteVilkår;
import no.nav.ung.sak.vilkår.VilkårUtleder;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class AktivitetspengerVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste, BehandlingstriggerPerioderTilVurderingTjeneste {

    // Årsaker som gjør at vilkåret vurderes på nytt for hele vilkårsperioden de treffer.
    private static final Set<BehandlingÅrsakType> VILKÅRSVURDERENDE_ÅRSAKER = Set.of(BehandlingÅrsakType.NY_SØKT_PERIODE);

    private AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder inngangsvilkårUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    AktivitetspengerVilkårsPerioderTilVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerVilkårsPerioderTilVurderingTjeneste(
        AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste,
        VilkårResultatRepository vilkårResultatRepository,
        @FagsakYtelseTypeRef(AKTIVITETSPENGER) @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD) VilkårUtleder inngangsvilkårUtleder,
        @FagsakYtelseTypeRef(AKTIVITETSPENGER) ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.aktivitetspengerSøknadsperiodeTjeneste = aktivitetspengerSøknadsperiodeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var vilkårsperioder = hentVilkårsperioder(behandlingId, vilkårType);
        if (vilkårsperioder.isEmpty()) {
            return aktivitetspengerSøknadsperiodeTjeneste.utledPeriode(behandlingId);
        }
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggere = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        LocalDateTimeline<Boolean> tilVurdering = perioderFraVilkårsvurderendeTriggere(prosesstriggere, VILKÅRSVURDERENDE_ÅRSAKER, vilkårsperioder.get())
            .crossJoin(avklartePerioder(behandlingId, vilkårType, vilkårsperioder.get()), StandardCombinators::alwaysTrueForMatch);

        // Komprimeres for hver vilkårsperiode for seg: knekkpunkter fra crossJoin og overlapp mot avklaringer fjernes
        return vilkårsperioder.get().stream()
            .flatMap(vilkårsperiode -> tilVurdering.intersection(vilkårsperiode.toLocalDateInterval()).compress().getLocalDateIntervals().stream())
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Map<BehandlingÅrsakType, LocalDateTimeline<Boolean>> utledPerTrigger(Long behandlingId, VilkårType vilkårType) {
        List<DatoIntervallEntitet> vilkårsperioder = hentVilkårsperioder(behandlingId, vilkårType).orElse(List.of());
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggere = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);

        var perioderPerÅrsak = new EnumMap<BehandlingÅrsakType, LocalDateTimeline<Boolean>>(BehandlingÅrsakType.class);
        VILKÅRSVURDERENDE_ÅRSAKER.forEach(årsak ->
            perioderPerÅrsak.put(årsak, perioderFraVilkårsvurderendeTriggere(prosesstriggere, Set.of(årsak), vilkårsperioder)));

        VilkårsavklaringÅrsaker.avklaringsårsakFor(vilkårType).ifPresent(årsak ->
            perioderPerÅrsak.put(årsak, avklartePerioder(behandlingId, vilkårType, vilkårsperioder))
        );

        Set<BehandlingÅrsakType> avklaringsårsaker = VilkårsavklaringÅrsaker.alleÅrsaker();
        prosesstriggere.stream()
            .flatMap(segment -> segment.getValue().stream())
            .filter(årsak -> !VILKÅRSVURDERENDE_ÅRSAKER.contains(årsak))
            .filter(årsak -> !avklaringsårsaker.contains(årsak))
            .collect(Collectors.toSet()).forEach(årsak ->
            perioderPerÅrsak.put(årsak, perioderFraTriggerSomIkkeVurdererVilkår(prosesstriggere, årsak, vilkårsperioder))
        );

        perioderPerÅrsak.values().removeIf(LocalDateTimeline::isEmpty);
        return perioderPerÅrsak;
    }

    private static LocalDateTimeline<Boolean> perioderFraVilkårsvurderendeTriggere(LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggere,
                                                                                   Set<BehandlingÅrsakType> årsaker,
                                                                                   List<DatoIntervallEntitet> vilkårsperioder) {
        var triggere = prosesstriggere.filterValue(årsakerPåSegment -> årsakerPåSegment.stream().anyMatch(årsaker::contains));
        return TidslinjeUtil.tilTidslinje(vilkårsperioder.stream()
            .filter(periode -> !triggere.intersection(periode.toLocalDateInterval()).isEmpty())
            .toList());
    }

    // Foreslåtte avklaringer gjelder ett vilkår og kopieres ikke til nye behandlinger, så de trenger ingen trigger.
    private LocalDateTimeline<Boolean> avklartePerioder(Long behandlingId, VilkårType vilkårType, List<DatoIntervallEntitet> vilkårsperioder) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, vilkårType)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
        return TidslinjeUtil.tilTidslinje(foreslåtteAvklaringer.stream().map(VilkårPeriodeAvklaring::getPeriode).toList())
            .intersection(TidslinjeUtil.tilTidslinje(vilkårsperioder));
    }

    private static LocalDateTimeline<Boolean> perioderFraTriggerSomIkkeVurdererVilkår(LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggere,
                                                                                     BehandlingÅrsakType årsak,
                                                                                     List<DatoIntervallEntitet> vilkårsperioder) {
        return prosesstriggere.filterValue(årsaker -> årsaker.contains(årsak))
            .mapValue(_ -> Boolean.TRUE)
            .intersection(TidslinjeUtil.tilTidslinje(vilkårsperioder));
    }

    private Optional<List<DatoIntervallEntitet>> hentVilkårsperioder(Long behandlingId, VilkårType vilkårType) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .flatMap(it -> it.getVilkår(vilkårType))
            .map(vilkår -> vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode).toList());
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        NavigableSet<DatoIntervallEntitet> perioder = utledPerioderFraRelevanteEndringer(behandlingId);
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        return utledeteVilkår.getAlleAvklarte().stream()
            .collect(Collectors.toMap(vilkårtype -> vilkårtype, _ -> perioder));
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }


    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        //for at ny søknad skal vurderes uten å gjenåpne perioder fra tidligere behandlinger
        return new IngenVurdering();
    }


    @Override
    public Set<VilkårType> definerendeVilkår() {
        //FIXME AKT. Dette er sannsynligvis ikke riktig vilkår (spesielt ikke dersom vilkåret flyttes til del2)
        return Set.of(VilkårType.ALDERSVILKÅR);
    }

    /**
     * Finner perioder som vurderes.
     * <p>
     * Endringer som medfører at perioden vurderes er
     * - Nye/endrede søknadsperioder fra bruker
     *
     * @param behandlingId BehandlingId
     * @return Perioder som vurderes
     */
    private NavigableSet<DatoIntervallEntitet> utledPerioderFraRelevanteEndringer(Long behandlingId) {
        return aktivitetspengerSøknadsperiodeTjeneste.utledPeriode(behandlingId);
    }
}
