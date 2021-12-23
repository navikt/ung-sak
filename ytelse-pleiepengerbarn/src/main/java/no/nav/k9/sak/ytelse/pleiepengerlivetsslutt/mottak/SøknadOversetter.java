package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.pls.v1.Pleietrengende;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;

@Dependent
class SøknadOversetter {

    private SøknadRepository søknadRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    SøknadOversetter(SøknadsperiodeRepository søknadsperiodeRepository,
                     BehandlingRepositoryProvider repositoryProvider,
                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                     TpsTjeneste tpsTjeneste) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.tpsTjeneste = tpsTjeneste;
    }


    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var mottattDato = søknad.getMottattDato().toLocalDate();

        PleipengerLivetsSluttfase ytelse = søknad.getYtelse();

        var mapper = new MapSøknadUttakPerioder(tpsTjeneste, søknad, ytelse, journalpostId);
        var perioderFraSøknad = mapper.getPerioderFraSøknad();

        final List<Periode> søknadsperioder= perioderFraSøknad.getArbeidPerioder().stream().map(it -> it.getPeriode()).map(di -> new Periode(di.getFomDato(), di.getTomDato())).collect(Collectors.toList());
        final var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        lagrePleietrengende(fagsakId, ytelse.getPleietrengende());
        lagreSøknadPerioder(perioderFraSøknad, journalpostId, behandlingId);
        lagreUttak(perioderFraSøknad, behandlingId);
        oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    private void lagreSøknadEntitet(Søknad søknad, JournalpostId journalpostId, Long behandlingId, Optional<Periode> maksSøknadsperiode, LocalDate mottattDato) {
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(maksSøknadsperiode.map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed())).orElse(null))
            .medElektroniskRegistrert(true)
            .medMottattDato(mottattDato)
            .medErEndringssøknad(false)
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSøknadsdato(maksSøknadsperiode.map(Periode::getFraOgMed).orElse(mottattDato))
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
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

    private void lagreUttak(PerioderFraSøknad perioderFraSøknad, Long behandlingId) {
        uttakPerioderGrunnlagRepository.lagre(behandlingId, perioderFraSøknad);
    }

    private void oppdaterFagsakperiode(Optional<Periode> maksSøknadsperiode, Long fagsakId) {
        maksSøknadsperiode.ifPresent(periode -> fagsakRepository.utvidPeriode(fagsakId, periode.getFraOgMed(), periode.getTilOgMed()));
    }

    private void lagreSøknadPerioder(PerioderFraSøknad perioderFraSøknad, JournalpostId journalpostId, Long behandlingId) {
        var søknadsperioder = perioderFraSøknad.getArbeidPerioder().stream()
            .map(s -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getPeriode().getFomDato(), s.getPeriode().getTomDato())))
            .collect(Collectors.toList());
        søknadsperiodeRepository.lagre(behandlingId, new Søknadsperioder(journalpostId, søknadsperioder));
    }

    private void lagrePleietrengende(Long fagsakId, Pleietrengende pleietrengende) {
        final var norskIdentitetsnummer = pleietrengende.getPersonIdent();
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.getVerdi())).orElseThrow();
            fagsakRepository.oppdaterPleietrengende(fagsakId, aktørId);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void lagreMedlemskapinfo(Bosteder bosteder, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        if (bosteder != null) {
            bosteder.getPerioder().forEach((periode, opphold) -> {
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.getLand().getLandkode()))
                    .medPeriode(
                        Objects.requireNonNull(periode.getFraOgMed()),
                        Objects.requireNonNullElse(periode.getTilOgMed(), Tid.TIDENES_ENDE))
                    .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
