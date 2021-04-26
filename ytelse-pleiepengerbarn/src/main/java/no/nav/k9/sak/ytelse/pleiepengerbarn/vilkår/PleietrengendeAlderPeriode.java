package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

public class PleietrengendeAlderPeriode implements VilkårsPeriodiseringsFunksjon {

    private static final int MAKSÅR = 200;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private int fomAlder;
    private int toAlder;

    
    private PleietrengendeAlderPeriode(SøknadsperiodeRepository søknadsperiodeRepository,
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            int fomAlder,
            int toAlder) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fomAlder = fomAlder;
        this.toAlder = toAlder;
    }
    

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {        
        final var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);
        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        }
        
        final var perioder = søknadsperioder.get().getPerioder();
        final var fødselsdato = finnPleietrengendesFødselsdato(behandlingId);
        final var periodeSomKanUtledes = new LocalDateInterval(fødselsdato.plusYears(fomAlder), fødselsdato.plusYears(toAlder).minusDays(1));
        
        final var tidslinje = tilTidslinje(perioder);
        final var resultat = tidslinje.intersection(periodeSomKanUtledes);
        return tilNavigableSet(resultat);
    }

    private LocalDate finnPleietrengendesFødselsdato(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(
            behandlingId,
            behandling.getFagsak().getAktørId(),
            behandling.getFagsak().getPeriode().getFomDato()
        );
        var pleietrengendePersonopplysning = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());
        return pleietrengendePersonopplysning.getFødselsdato();
    }
    
    private LocalDateTimeline<Boolean> tilTidslinje(Set<Søknadsperioder> perioder) {
        return new LocalDateTimeline<Boolean>(
                perioder.stream()
                    .map(Søknadsperioder::getPerioder)
                    .flatMap(Collection::stream)
                    .map(Søknadsperiode::getPeriode)
                    .map(d -> new LocalDateSegment<Boolean>(d.getFomDato(), d.getTomDato(), Boolean.TRUE))
                    .collect(Collectors.toList())
            );
    }
    
    private TreeSet<DatoIntervallEntitet> tilNavigableSet(LocalDateTimeline<Boolean> resultat) {
        return resultat
          .stream()
          .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
          .collect(Collectors.toCollection(TreeSet::new));
    }
    
    public static final PleietrengendeAlderPeriode under18(SøknadsperiodeRepository søknadsperiodeRepository,
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository) {
        return new PleietrengendeAlderPeriode(søknadsperiodeRepository, personopplysningTjeneste, behandlingRepository, -MAKSÅR, 18);
    }
    
    public static final PleietrengendeAlderPeriode overEllerLik18(SøknadsperiodeRepository søknadsperiodeRepository,
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository) {
        return new PleietrengendeAlderPeriode(søknadsperiodeRepository, personopplysningTjeneste, behandlingRepository, 18, MAKSÅR);
    }
}
