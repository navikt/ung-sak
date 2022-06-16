package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@Dependent
class SøknadOversetter {

    private TpsTjeneste tpsTjeneste;
    private SøknadPersisterer søknadPersisterer;

    SøknadOversetter() {
        // for CDI proxy
    }

    @Inject
    SøknadOversetter(TpsTjeneste tpsTjeneste,
                     SøknadPersisterer søknadPersisterer) {
        this.tpsTjeneste = tpsTjeneste;
        this.søknadPersisterer = søknadPersisterer;
    }

    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var mottattDato = søknad.getMottattDato().toLocalDate();

        PleiepengerSyktBarn ytelse = søknad.getYtelse();
        var perioderFraSøknad = getPerioderFraSøknad(ytelse, journalpostId);

        final List<Periode> søknadsperioder = hentAlleSøknadsperioder(ytelse);
        final var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        // Felles Pleiepenger
        søknadPersisterer.lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        søknadPersisterer.lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        søknadPersisterer.lagrePleietrengende(fagsakId, ytelse.getPleietrengende().getPersonIdent());
        søknadPersisterer.lagreSøknadsperioder(søknadsperioder, ytelse.getTrekkKravPerioder(), journalpostId, behandlingId);
        søknadPersisterer.lagreUttak(perioderFraSøknad, behandlingId);
        // Kun for PSB
        søknadPersisterer.lagreBeredskapOgNattevåk(søknad, behandlingId);
        søknadPersisterer.lagreOmsorg(ytelse.getOmsorg(), søknadsperioder, behandling);
        // Utvid periode til slutt
        søknadPersisterer.oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    private Optional<Periode> finnMaksperiode(List<Periode> perioder) {
        if (perioder == null || perioder.isEmpty()) {
            return Optional.empty();
        }
        final var fom = perioder
            .stream()
            .map(Periode::getFraOgMed)
            .min(LocalDate::compareTo)
            .orElseThrow();
        final var tom = perioder
            .stream()
            .map(Periode::getTilOgMed)
            .max(LocalDate::compareTo)
            .orElseThrow();
        return Optional.of(new Periode(fom, tom));
    }

    private List<Periode> hentAlleSøknadsperioder(PleiepengerSyktBarn ytelse) {
        final LocalDateTimeline<Boolean> kompletteSøknadsperioderTidslinje = tilTidslinje(ytelse.getSøknadsperiodeList());
        final var endringsperioder = ytelse.getEndringsperiode();
        final LocalDateTimeline<Boolean> endringssøknadsperioderTidslinje = tilTidslinje(endringsperioder);
        final LocalDateTimeline<Boolean> søknadsperioder = kompletteSøknadsperioderTidslinje.union(endringssøknadsperioderTidslinje, StandardCombinators::coalesceLeftHandSide).compress();
        return søknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
    }

    private LocalDateTimeline<Boolean> tilTidslinje(List<Periode> perioder) {
        return new LocalDateTimeline<>(
            perioder.stream()
                .map(p -> new LocalDateSegment<>(p.getFraOgMed(), p.getTilOgMed(), Boolean.TRUE))
                .collect(Collectors.toList())
        ).compress();
    }

    private PerioderFraSøknad getPerioderFraSøknad(PleiepengerSyktBarn ytelse, JournalpostId journalpostId) {
        var mapper = new MapSøknadUttakPerioder(tpsTjeneste);
        return new PerioderFraSøknad(journalpostId,
            mapper.mapUttak(ytelse.getUttak()),
            mapper.mapOppgittArbeidstid(ytelse.getArbeidstid()),
            mapper.mapOppgittTilsynsordning(ytelse.getTilsynsordning()),
            mapper.mapUtenlandsopphold(ytelse.getUtenlandsopphold()),
            mapper.mapFerie(ytelse.getSøknadsperiodeList(), ytelse.getLovbestemtFerie()),
            mapper.mapBeredskap(ytelse.getBeredskap()),
            mapper.mapNattevåk(ytelse.getNattevåk()));
    }
}
