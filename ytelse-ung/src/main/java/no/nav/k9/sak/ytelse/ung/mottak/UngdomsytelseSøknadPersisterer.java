package no.nav.k9.sak.ytelse.ung.mottak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiode;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperioder;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;

@Dependent
public class UngdomsytelseSøknadPersisterer {

    private final SøknadRepository søknadRepository;
    private final FagsakRepository fagsakRepository;
    private final UngdomsytelseSøknadsperiodeRepository ungdomsytelseSøknadsperiodeRepository;


    @Inject
    public UngdomsytelseSøknadPersisterer(BehandlingRepositoryProvider repositoryProvider, FagsakRepository fagsakRepository,
                                   UngdomsytelseSøknadsperiodeRepository ungdomsytelseSøknadsperiodeRepository) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = fagsakRepository;
        this.ungdomsytelseSøknadsperiodeRepository = ungdomsytelseSøknadsperiodeRepository;
    }


    // TODO: Trenger vi å lagre ned en egen søknadentitet for ungdomsytelsen? Er relevant dersom vi skal vurdere kompletthet for søknad (f.eks om søknad har kommet inn for tidlig)
    public void lagreSøknadEntitet(Søknad søknad, JournalpostId journalpostId, Long behandlingId, Optional<Periode> maksSøknadsperiode, LocalDate mottattDato) {
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

    public void lagreSøknadsperioder(List<Periode> søknadsperioder, JournalpostId journalpostId, Long behandlingId) {
        final List<UngdomsytelseSøknadsperiode> søknadsperiodeliste = new ArrayList<>();
        søknadsperioder.stream()
            .map(s -> new UngdomsytelseSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFraOgMed(), s.getTilOgMed())))
            .forEach(søknadsperiodeliste::add);

        ungdomsytelseSøknadsperiodeRepository.lagre(behandlingId, new UngdomsytelseSøknadsperioder(journalpostId, søknadsperiodeliste.toArray(new UngdomsytelseSøknadsperiode[0])));
    }


    /** Utvider fagsakperiode i en eller begge ender fra fom og tom i oppgitt perioder dersom denne medfører en større periode.
     * @param maksSøknadsperiode Ny utvidelse av fagsakperioder
     * @param fagsakId FagsakId
     */
    public void oppdaterFagsakperiode(Optional<Periode> maksSøknadsperiode, Long fagsakId) {
        maksSøknadsperiode.ifPresent(periode -> fagsakRepository.utvidPeriode(fagsakId, periode.getFraOgMed(), periode.getTilOgMed()));
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }
}
