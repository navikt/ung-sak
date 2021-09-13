package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

public class PleietrengendeAlderPeriode implements VilkårsPeriodiseringsFunksjon {
    
    public static final int ALDER_FOR_STRENGERE_PSB_VURDERING = 18;

    private static final int MAKSÅR = 200;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;
    
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private int fomAlder;
    private int toAlder;

    
    private PleietrengendeAlderPeriode(BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            int fomAlder,
            int toAlder) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.fomAlder = fomAlder;
        this.toAlder = toAlder;
    }
    

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        // XXX: Hvorfor virker det ikke med utledPeriode her?
        final var perioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId);
        
        final var fødselsdato = finnPleietrengendesFødselsdato(behandlingId);
        final var periodeSomKanUtledes = new LocalDateInterval(fødselsdato.plusYears(fomAlder), fødselsdato.plusYears(toAlder).minusDays(1));
        
        final var tidslinje = SykdomUtils.toLocalDateTimeline(perioder);
        final var resultat = tidslinje.intersection(periodeSomKanUtledes);
        return tilNavigableSet(resultat);
    }

    private LocalDate finnPleietrengendesFødselsdato(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(
            behandlingId,
            behandling.getFagsak().getAktørId(),
            behandling.getFagsak().getPeriode().getFomDato()
        );
        
        if (personopplysningerAggregat.isEmpty()) {
            /*
             * Registerdatainnhenting har ikke blitt utført. Data må derfor hentes direkte fra PDL.
             */
            return fallbackDirekteoppslagFødselsdato(behandling.getFagsak().getPleietrengendeAktørId());
        }
        
        var pleietrengendePersonopplysning = personopplysningerAggregat.get().getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());
        return pleietrengendePersonopplysning.getFødselsdato();
    }
    
    private LocalDate fallbackDirekteoppslagFødselsdato(AktørId pleietrengende) {
        final var personinfo = personinfoAdapter.hentPersoninfo(pleietrengende);
        return personinfo.getFødselsdato();
    }
    
    private TreeSet<DatoIntervallEntitet> tilNavigableSet(LocalDateTimeline<Boolean> resultat) {
        return resultat
          .stream()
          .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
          .collect(Collectors.toCollection(TreeSet::new));
    }
    
    public static final PleietrengendeAlderPeriode under18(
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        return new PleietrengendeAlderPeriode(personopplysningTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
    }
    
    public static final PleietrengendeAlderPeriode overEllerLik18(
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        return new PleietrengendeAlderPeriode(personopplysningTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);
    }
}
