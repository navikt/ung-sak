package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.MapSøknadUttakPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.SøknadPersisterer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;
import no.nav.k9.søknad.ytelse.psb.v1.Uttak;

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

        Collection<ArbeidPeriode> arbeidPerioder = new MapSøknadUttakPerioder(tpsTjeneste).mapOppgittArbeidstid(ytelse.getArbeidstid());
        List<Periode> søknadsperioder;
        Collection<UttakPeriode> uttakPerioder;
        if (ytelse.getSøknadsperiodeList().isEmpty()) {
            throw new IllegalStateException("Støtter midlertidig ikke søknad uten søknadsperioder for pleiepenger i livets sluttfase. Jobbes med!");
        } else {
            søknadsperioder = ytelse.getSøknadsperiodeList();
            uttakPerioder = mapUttak(ytelse.getUttak());
        }
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            uttakPerioder,
            arbeidPerioder,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of());

        var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        søknadPersisterer.lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        søknadPersisterer.lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        søknadPersisterer.lagrePleietrengende(fagsakId, ytelse.getPleietrengende().getPersonIdent());
        søknadPersisterer.lagreSøknadsperioder(søknadsperioder, ytelse.getTrekkKravPerioder(), journalpostId, behandlingId);
        søknadPersisterer.lagreUttak(perioderFraSøknad, behandlingId);
        søknadPersisterer.oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    Collection<UttakPeriode> mapUttak(Uttak uttak) {
        if (uttak == null || uttak.getPerioder() == null) {
            return List.of();
        }
        return uttak.getPerioder()
            .entrySet()
            .stream()
            .map(it -> new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFraOgMed(), it.getKey().getTilOgMed()), it.getValue().getTimerPleieAvBarnetPerDag())).collect(Collectors.toList());
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
