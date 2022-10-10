package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Periodetype;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class OMPVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new EnumMap<>(VilkårType.class);
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private NulledePerioder nulledePerioder;
    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;

    OMPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OMPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(OMSORGSPENGER) VilkårUtleder vilkårUtleder,
                                                  OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  TrekkUtFraværTjeneste trekkUtFraværTjeneste,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  ÅrskvantumTjeneste årskvantumTjeneste,
                                                  ProsessTriggereRepository prosessTriggereRepository) {
        this.vilkårUtleder = vilkårUtleder;
        søktePerioder = new SøktePerioder(omsorgspengerGrunnlagRepository);
        nulledePerioder = new NulledePerioder(omsorgspengerGrunnlagRepository);
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;

        var maksSøktePeriode = new MaksSøktePeriode(omsorgspengerGrunnlagRepository);
        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
        vilkårsPeriodisering.put(VilkårType.OPPTJENINGSVILKÅRET, søktePerioder);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, søktePerioder);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fraværPåSak = trekkUtFraværTjeneste.alleFraværsperioderPåFagsak(behandling);
        List<OppgittFraværPeriode> fraværsperioderSak = KravDokumentFravær.mapTilOppgittFraværPeriode(fraværPåSak);

        // filtrer bort perioder som ikke kan tilbakestilles pga andre krav fra andre arbeidsgivere på samme dato
        return this.nulledePerioder.utledPeriode(behandlingId, fraværsperioderSak);
    }


    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fraværPåSak = trekkUtFraværTjeneste.alleFraværsperioderPåFagsak(behandling);
        List<OppgittFraværPeriode> fraværsperioderSak = KravDokumentFravær.mapTilOppgittFraværPeriode(fraværPåSak);

        return søktePerioder.utledPeriodeFraSøknadsPerioder(fraværsperioderSak);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var perioder = utledPeriode(behandlingId, vilkårType);
        var perioderSomSkalTilbakestilles = Collections.unmodifiableNavigableSet(nulledePerioder.utledPeriode(behandlingId)
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new)));
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(behandling, vilkårene.get(), perioder, perioderSomSkalTilbakestilles);
        }
        return perioder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        var vilkårType = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
        var behandlingId = referanse.getBehandlingId();
        var perioder = utled(behandlingId, vilkårType);
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .flatMap(it -> it.getVilkår(vilkårType));

        if (vilkår.isEmpty()) {
            return new TreeSet<>();
        }

        var vilkårsPerioder = vilkår.get().getPerioder().stream().map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        var fullUttaksplan = årskvantumTjeneste.hentFullUttaksplan(referanse.getSaksnummer());

        var aktivitetsperioder = fullUttaksplan.getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .filter(it -> Periodetype.REVURDERT.equals(it.getPeriodetype()))
            .map(Uttaksperiode::getPeriode)
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .filter(it -> perioder.stream().noneMatch(it::overlapper))
            .collect(Collectors.toCollection(TreeSet::new));

        return vilkårsPerioder.stream()
            .filter(it -> aktivitetsperioder.stream()
                .anyMatch(it::overlapper))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Behandling behandling, Vilkår vilkår, Set<DatoIntervallEntitet> perioder,
                                                                                           NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());

        return vilkår.getPerioder()
            .stream()
            .filter(it -> perioder.stream().anyMatch(p -> it.getPeriode().overlapper(p))
                || overlapperMedÅrsakPeriode(it, prosessTriggere)
                || perioderSomSkalTilbakestilles.stream().anyMatch(p -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato().minusDays(1), p.getTomDato().plusDays(1))))
                || perioderSomSkalTilbakestilles.stream().anyMatch(p -> erKantIKantVurderer.erKantIKant(it.getPeriode(), p)))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean overlapperMedÅrsakPeriode(VilkårPeriode it, Optional<ProsessTriggere> triggere) {
        if (triggere.isEmpty()) {
            return false;
        }
        return triggere.get().getTriggere().stream()
            .filter(årsak -> Objects.nonNull(årsak.getPeriode()))
            .anyMatch(at -> it.getPeriode().overlapper(at.getPeriode()));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkårType) {
        return vilkårsPeriodisering.getOrDefault(vilkårType, søktePerioder).utledPeriode(behandlingId);
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
    }
}
