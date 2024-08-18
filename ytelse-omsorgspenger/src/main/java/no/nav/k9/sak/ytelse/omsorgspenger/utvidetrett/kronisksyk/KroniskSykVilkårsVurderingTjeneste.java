package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
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

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@BehandlingTypeRef
@RequestScoped
public class KroniskSykVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(KroniskSykVilkårsVurderingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private UtvidetRettSøknadPerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder vilkårUtleder;

    KroniskSykVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public KroniskSykVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
                                              @FagsakYtelseTypeRef(OMSORGSPENGER_KS) VilkårUtleder vilkårUtleder,
                                              VilkårResultatRepository vilkårResultatRepository,
                                              PersoninfoAdapter personinfoAdapter,
                                              SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søktePerioder = new UtvidetRettSøknadPerioder(søknadRepository);
        this.vilkårUtleder = vilkårUtleder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        return new TreeSet<>();
    }

    @WithSpan
    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(@SpanAttribute("behandlingId") Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        AktørId barnAktørId = fagsak.getPleietrengendeAktørId();
        var søknadsperioder = søktePerioder.utledPeriode(behandlingId);
        var maksPeriode = utledMaksPeriode(søknadsperioder, barnAktørId);
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(maksPeriode)));
    }

    @WithSpan
    @Override
    public NavigableSet<DatoIntervallEntitet> utled(@SpanAttribute("behandlingId") Long behandlingId, @SpanAttribute("vilkarType") VilkårType vilkårType) {
        var optVilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (optVilkårene.isPresent()) {
            var vilkårTidslinje = optVilkårene.get().getVilkårTimeline(vilkårType);
            return TidslinjeUtil.tilDatoIntervallEntiteter(vilkårTidslinje);
        } else {
            // default til 'fullstedige' perioder hvis vilkår ikke angitt.
            return utledFullstendigePerioder(behandlingId);
        }
    }

    @WithSpan
    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(@SpanAttribute("behandlingId") Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledFullstendigePerioder(behandlingId)));

        return vilkårPeriodeSet;
    }

    private DatoIntervallEntitet utledMaksPeriode(NavigableSet<DatoIntervallEntitet> søktePerioder, AktørId barnAktørId) {
        var barninfo = personinfoAdapter.hentBrukerBasisForAktør(barnAktørId).orElseThrow(() -> new IllegalStateException("Mangler personinfo for pleietrengende aktørId"));

        // ikke åpne fagsaken før barnets fødselsdato
        var fødselsdato = barninfo.getFødselsdato();
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid
        var førsteSøktePeriode = søktePerioder.first();
        LocalDate søknadFom = førsteSøktePeriode.getFomDato();
        var fristFørSøknadsdato = søknadFom.minusYears(3).withMonth(1).withDayOfMonth(1);

        var mindato = List.of(fødselsdato, fristFørSøknadsdato).stream().max(LocalDate::compareTo).get();

        // kan ikke gå lenger enn til 18 år (kun oppfylt i årskvantum om kronisk syk også fins
        var maksdato = barninfo.getFødselsdato().plusYears(18).withMonth(12).withDayOfMonth(31);

        if (maksdato.isBefore(mindato) || søknadFom.isAfter(maksdato)) {
            log.warn("Har ingen reell periode å vurdere. mindato {}, maksdato {}, søknadsdato {}", mindato, maksdato, søknadFom);
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadFom, søknadFom);
        } else {
            return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
        }
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }

    public DatoIntervallEntitet utledMaksPeriode(DatoIntervallEntitet periode, AktørId pleietrengendeAktørId) {
        return utledMaksPeriode(new TreeSet<>(Set.of(periode)), pleietrengendeAktørId);
    }
}
