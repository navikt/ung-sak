package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.MapSøknadUttakPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.SøknadPersisterer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;

@Dependent
class SøknadOversetter {

    private TpsTjeneste tpsTjeneste;
    private SøknadPersisterer søknadPersisterer;

    @Inject
    SøknadOversetter(TpsTjeneste tpsTjeneste, SøknadPersisterer søknadPersisterer) {
        this.tpsTjeneste = tpsTjeneste;
        this.søknadPersisterer = søknadPersisterer;
    }


    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var mottattDato = søknad.getMottattDato().toLocalDate();
        PleipengerLivetsSluttfase ytelse = søknad.getYtelse();

        var arbeidPerioder = new MapSøknadUttakPerioder(tpsTjeneste).mapOppgittArbeidstid(ytelse.getArbeidstid());
        var perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            mapTilUttakPerioder(arbeidPerioder),
            arbeidPerioder,
            List.of(),
            List.of(),
            List.of(),
            List.of());

        var søknadsperioder = arbeidPerioder.stream().map(it -> it.getPeriode()).map(di -> new Periode(di.getFomDato(), di.getTomDato())).collect(Collectors.toList());
        var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        søknadPersisterer.lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        søknadPersisterer.lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        søknadPersisterer.lagrePleietrengende(fagsakId, ytelse.getPleietrengende().getPersonIdent());
        søknadPersisterer.lagreSøknadsperioder(søknadsperioder, List.of(), journalpostId, behandlingId);
        søknadPersisterer.lagreUttak(perioderFraSøknad, behandlingId);
        søknadPersisterer.oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    private Collection<UttakPeriode> mapTilUttakPerioder(Collection<ArbeidPeriode> arbeidPerioder) {
        Collection<UttakPeriode> uttaksperioder = arbeidPerioder.stream()
            .map(arbeidPeriode -> {
                // Omregner arbeidsperiode til uttaksperiode
                var jobberNormaltTimerPerDag = arbeidPeriode.getJobberNormaltTimerPerDag();
                var faktiskArbeidTimerPerDag = arbeidPeriode.getFaktiskArbeidTimerPerDag();
                var fraværTimerPerDag = jobberNormaltTimerPerDag.minus(faktiskArbeidTimerPerDag);
                return new UttakPeriode(arbeidPeriode.getPeriode(), fraværTimerPerDag);
            })
            .toList();
        return uttaksperioder;
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
}
