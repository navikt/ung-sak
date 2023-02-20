package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NavigableSet;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

public class PleietrengendeAlderPeriode implements VilkårsPeriodiseringsFunksjon {

    public static final int ALDER_FOR_STRENGERE_PSB_VURDERING = 18;

    public static final int MAKSÅR = 200;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private boolean brukRelevantPeriode;
    private int fomAlder;
    private int toAlder;


    private PleietrengendeAlderPeriode(BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            boolean brukRelevantPeriode,
            int fomAlder,
            int toAlder) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.brukRelevantPeriode = brukRelevantPeriode;
        this.fomAlder = fomAlder;
        this.toAlder = toAlder;
    }


    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        /*
         * Denne sjekken kan fjernes når det ikke lenger finnes noen åpne
         * behandlinger opprettet før angitt tidspunkt.
         */
        final boolean opprettetEtterNyHåndtering = behandling.getOpprettetTidspunkt().isAfter(LocalDateTime.of(2021, 11, 30, 21, 15, 0));

        final NavigableSet<DatoIntervallEntitet> perioder;
        if (brukRelevantPeriode && opprettetEtterNyHåndtering) {
            perioder = søknadsperiodeTjeneste.utledPeriode(behandlingId);
        } else {
            perioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId);
        }

        final var fødselsdato = finnPleietrengendesFødselsdato(behandling);
        return utledPeriodeIHenhold(perioder, fødselsdato, fomAlder, toAlder);
    }

    public static NavigableSet<DatoIntervallEntitet> utledPeriodeIHenhold(NavigableSet<DatoIntervallEntitet> perioder, LocalDate fødselsdato, int fomAlder, int toAlder) {
        final var periodeSomKanUtledes = new LocalDateInterval(fødselsdato.plusYears(fomAlder), fødselsdato.plusYears(toAlder).minusDays(1));

        final var tidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioder);
        final var resultat = tidslinje.intersection(periodeSomKanUtledes);
        return tilNavigableSet(resultat);
    }

    private LocalDate finnPleietrengendesFødselsdato(Behandling behandling) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(
            behandling.getId(),
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

    private static NavigableSet<DatoIntervallEntitet> tilNavigableSet(LocalDateTimeline<Boolean> resultat) {
        return TidslinjeUtil.tilDatoIntervallEntiteter(resultat);
    }

    public static final PleietrengendeAlderPeriode under18(
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            boolean brukRelevantPeriode) {
        return new PleietrengendeAlderPeriode(personopplysningTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode, -MAKSÅR, ALDER_FOR_STRENGERE_PSB_VURDERING);
    }

    public static final PleietrengendeAlderPeriode overEllerLik18(
            BasisPersonopplysningTjeneste personopplysningTjeneste,
            BehandlingRepository behandlingRepository,
            PersoninfoAdapter personinfoAdapter,
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            boolean brukRelevantPeriode) {
        return new PleietrengendeAlderPeriode(personopplysningTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste,brukRelevantPeriode, ALDER_FOR_STRENGERE_PSB_VURDERING, MAKSÅR);
    }
}
