package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.StandardCombinators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.UtvidetRettSøknadPerioder;

@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER_AO)
@BehandlingTypeRef
@ApplicationScoped
public class AleneomsorgVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(AleneomsorgVilkårsPerioderTilVurderingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder vilkårUtleder;
    private PersoninfoAdapter personinfoAdapter;
    private UtvidetRettSøknadPerioder søktePerioder;

    AleneomsorgVilkårsPerioderTilVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public AleneomsorgVilkårsPerioderTilVurderingTjeneste(BehandlingRepository behandlingRepository,
                                                          VilkårResultatRepository vilkårResultatRepository,
                                                          @FagsakYtelseTypeRef(OMSORGSPENGER_AO) VilkårUtleder vilkårUtleder,
                                                          PersoninfoAdapter personinfoAdapter,
                                                          SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadRepository = søknadRepository;
        this.søktePerioder = new UtvidetRettSøknadPerioder(søknadRepository);
        this.vilkårUtleder = vilkårUtleder;
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
            return TidslinjeUtil.tilDatoIntervallEntiteter(vilkårTidslinje);
        } else {
            return utledVilkårsperioder(behandlingId);
        }
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledVilkårsperioder(behandlingId)));

        return vilkårPeriodeSet;
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsperioder(Long behandlingId) {
        var søknadsperioder = søktePerioder.utledPeriode(behandlingId);
        var søknadsperioderEtterBarnetsFødsel = justerTilDefaultAlder(behandlingId, søknadsperioder);
        return justerForMottattTidspunkt(behandlingId, søknadsperioderEtterBarnetsFødsel);
    }

    private NavigableSet<DatoIntervallEntitet> justerForMottattTidspunkt(Long behandlingId, NavigableSet<DatoIntervallEntitet> søknadsperiode) {
        LocalDateTimeline<?> søknadstidslinje = new LocalDateTimeline<>(søknadsperiode.stream().map(sp -> new LocalDateSegment<>(sp.getFomDato(), sp.getTomDato(), true)).toList());

        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingId);
        LocalDate mottattDato = søknad.getMottattDato();
        LocalDate startAvÅretForTremånederSidenDato = mottattDato.withDayOfMonth(1).minusMonths(3).with(TemporalAdjusters.firstDayOfYear());
        LocalDateTimeline<?> justert = søknadstidslinje.intersection(new LocalDateTimeline<>(startAvÅretForTremånederSidenDato, LocalDate.MAX, null));
        return TidslinjeUtil.tilDatoIntervallEntiteter(justert);
    }

    private NavigableSet<DatoIntervallEntitet> justerTilDefaultAlder(Long behandlingId, NavigableSet<DatoIntervallEntitet> vilårsperiodeUtvidetRett) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        AktørId barnAktørId = fagsak.getPleietrengendeAktørId();
        var barninfo = personinfoAdapter.hentBrukerBasisForAktør(barnAktørId).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        // sett 'tom' for utvidet rett til barnet fyller 18 år by default
        var _18år = new LocalDateTimeline<>(barninfo.getFødselsdato(), barninfo.getFødselsdato().plusYears(18).withMonth(12).withDayOfMonth(31), Boolean.TRUE);
        var sammenstiltUtvidetRettTimeline = _18år.intersection(new LocalDateInterval(vilårsperiodeUtvidetRett.first().getFomDato(), vilårsperiodeUtvidetRett.last().getTomDato()));

        if (sammenstiltUtvidetRettTimeline.isEmpty()) {
            // K9-sak håndterer ikke tom vilkårsperiode, defaulter derfor til første dag slik at saksbehandler kan avslå denne
            LOGGER.info("Ingen overlapp mellom søknadsperiode og periode der barnet er under 18 år. Setter periode til " + vilårsperiodeUtvidetRett.first().getTomDato());
            return new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(vilårsperiodeUtvidetRett.first().getFomDato(), vilårsperiodeUtvidetRett.first().getFomDato())));
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(sammenstiltUtvidetRettTimeline);
    }

    private DatoIntervallEntitet utledMaksPeriode(NavigableSet<DatoIntervallEntitet> søktePerioder, AktørId barnAktørId) {
        var barninfo = personinfoAdapter.hentBrukerBasisForAktør(barnAktørId).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        // ikke åpne fagsaken før barnets fødselsdato
        var fødselsdato = barninfo.getFødselsdato();
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid
        var førsteSøktePeriode = søktePerioder.first();
        var fristFørSøknadsdato = førsteSøktePeriode.getFomDato().minusYears(3).withMonth(1).withDayOfMonth(1);

        var mindato = Stream.of(fødselsdato, fristFørSøknadsdato).max(LocalDate::compareTo).orElseThrow();
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
