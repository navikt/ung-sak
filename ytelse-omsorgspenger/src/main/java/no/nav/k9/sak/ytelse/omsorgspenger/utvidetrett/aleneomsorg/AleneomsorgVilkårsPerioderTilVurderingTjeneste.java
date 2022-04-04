package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.UtvidetRettSøknadPerioder;

@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER_AO)
@BehandlingTypeRef
@RequestScoped
public class AleneomsorgVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private PersoninfoAdapter personinfoAdapter;

    private UtvidetRettSøknadPerioder søktePerioder;

    AleneomsorgVilkårsPerioderTilVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public AleneomsorgVilkårsPerioderTilVurderingTjeneste(BehandlingRepository behandlingRepository,
                                                          VilkårResultatRepository vilkårResultatRepository,
                                                          PersoninfoAdapter personinfoAdapter,
                                                          SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søktePerioder = new UtvidetRettSøknadPerioder(søknadRepository);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        return new TreeSet<>();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        AktørId barnAktørId = fagsak.getPleietrengendeAktørId();
        var søknadsperioder = søktePerioder.utledPeriode(behandlingId);
        var maksPeriode = utledMaksPeriode(søknadsperioder, barnAktørId);
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(maksPeriode)));
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var optVilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (optVilkårene.isPresent()) {
            var vilkårTidslinje = optVilkårene.get().getVilkårTimeline(vilkårType);
            if (vilkårTidslinje.isEmpty()) {
                return Collections.emptyNavigableSet();
            }
            var utlededePerioder = vilkårTidslinje.getLocalDateIntervals().stream().map(p -> DatoIntervallEntitet.fra(p)).collect(Collectors.toCollection(TreeSet::new));
            return Collections.unmodifiableNavigableSet(utlededePerioder);
        } else {
            // default til 'fullstedige' perioder hvis vilkår ikke angitt.
            return utledFullstendigePerioder(behandlingId);
        }
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        return Map.of(
            VilkårType.UTVIDETRETT, justerTilDefaultAlder(behandlingId, utled(behandlingId, VilkårType.UTVIDETRETT)),
            VilkårType.OMSORGEN_FOR, utled(behandlingId, VilkårType.OMSORGEN_FOR));
    }

    private NavigableSet<DatoIntervallEntitet> justerTilDefaultAlder(Long behandlingId, NavigableSet<DatoIntervallEntitet> vilårsperiodeUtvidetRett) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        AktørId barnAktørId = fagsak.getPleietrengendeAktørId();
        var barninfo = personinfoAdapter.hentBrukerBasisForAktør(barnAktørId).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        // sett 'tom' for utvidet rett til barnet fyller 18 år by default
        var _18år = new LocalDateTimeline<>(barninfo.getFødselsdato(), barninfo.getFødselsdato().plusYears(18).withMonth(12).withDayOfMonth(31), Boolean.TRUE);
        var sammenstiltUtvidetRettTimeline = _18år.intersection(new LocalDateInterval(vilårsperiodeUtvidetRett.first().getFomDato(), vilårsperiodeUtvidetRett.last().getTomDato()));
        var utvidetRettPerioder = DatoIntervallEntitet.fraTimeline(sammenstiltUtvidetRettTimeline);
        return utvidetRettPerioder;
    }

    DatoIntervallEntitet utledMaksPeriode(NavigableSet<DatoIntervallEntitet> søktePerioder, AktørId barnAktørId) {
        var barninfo = personinfoAdapter.hentBrukerBasisForAktør(barnAktørId).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        // ikke åpne fagsaken før barnets fødselsdato
        var fødselsdato = barninfo.getFødselsdato();
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid
        var førsteSøktePeriode = søktePerioder.first();
        var fristFørSøknadsdato = førsteSøktePeriode.getFomDato().minusYears(3).withMonth(1).withDayOfMonth(1);

        var mindato = Set.of(fødselsdato, fristFørSøknadsdato).stream().max(LocalDate::compareTo).get();
        var maksdato = Tid.TIDENES_ENDE;
        return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }

    public DatoIntervallEntitet utledMaksPeriode(DatoIntervallEntitet periode, AktørId pleietrengendeAktørId) {
        return utledMaksPeriode(new TreeSet<>(Set.of(periode)), pleietrengendeAktørId);
    }
}
