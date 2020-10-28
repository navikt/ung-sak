package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Periodetype;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OMPVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private NulledePerioder nulledePerioder;
    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    OMPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OMPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("OMP") VilkårUtleder vilkårUtleder,
                                                  OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  TrekkUtFraværTjeneste trekkUtFraværTjeneste,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  ÅrskvantumTjeneste årskvantumTjeneste) {
        this.vilkårUtleder = vilkårUtleder;
        søktePerioder = new SøktePerioder(omsorgspengerGrunnlagRepository);
        nulledePerioder = new NulledePerioder(omsorgspengerGrunnlagRepository);
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;

        var maksSøktePeriode = new MaksSøktePeriode(omsorgspengerGrunnlagRepository);
        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
        vilkårsPeriodisering.put(VilkårType.OPPTJENINGSVILKÅRET, søktePerioder);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, søktePerioder); // Støtter da bare et skjæringstidspunkt per behandling
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        var fraværPåSak = trekkUtFraværTjeneste.fraværFraInntektsmeldingerPåFagsak(behandlingRepository.hentBehandling(behandlingId));
        // filtrer bort perioder som ikke kan tilbakestilles pga andre krav fra andre arbeidsgivere på samme dato
        return nulledePerioder.utledPeriode(behandlingId, fraværPåSak);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        var fraværPåSak = new HashSet<>(trekkUtFraværTjeneste.fraværFraInntektsmeldingerPåFagsak(behandlingRepository.hentBehandling(behandlingId)));

        return søktePerioder.utledPeriodeFraSøknadsPerioder(fraværPåSak);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var perioder = utledPeriode(behandlingId, vilkårType);
        var perioderSomSkalTilbakestilles = Collections.unmodifiableNavigableSet(nulledePerioder.utledPeriode(behandlingId)
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new)));
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(vilkårene.get(), perioder, perioderSomSkalTilbakestilles);
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

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Vilkår vilkår, Set<DatoIntervallEntitet> perioder,
                                                                                           NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {
        return vilkår.getPerioder()
            .stream()
            .filter(it -> perioder.stream().anyMatch(p -> it.getPeriode().overlapper(p))
                || perioderSomSkalTilbakestilles.stream().anyMatch(p -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato().minusDays(1), p.getTomDato().plusDays(1))))
                || perioderSomSkalTilbakestilles.stream().anyMatch(p -> erKantIKantVurderer.erKantIKant(it.getPeriode(), p)))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, søktePerioder).utledPeriode(behandlingId);
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
    }
}
