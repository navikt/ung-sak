package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FagsakperiodeUtleder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Dependent
public class UngdomsytelseSøknadPersisterer {

    private final SøknadRepository søknadRepository;
    private final FagsakRepository fagsakRepository;
    private final FagsakperiodeUtleder fagsakperiodeUtleder;
    private final UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;


    @Inject
    public UngdomsytelseSøknadPersisterer(BehandlingRepositoryProvider repositoryProvider, FagsakRepository fagsakRepository,
                                          FagsakperiodeUtleder fagsakperiodeUtleder,
                                          UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = fagsakRepository;
        this.fagsakperiodeUtleder = fagsakperiodeUtleder;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
    }


    public void lagreSøknadEntitet(Søknad søknad, JournalpostId journalpostId, Long behandlingId, LocalDate startdato, LocalDate mottattDato) {
        var søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(true)
            .medMottattDato(mottattDato)
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medStartdato(startdato)
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    public void lagreSøknadsperioder(List<LocalDate> startDatoer, JournalpostId journalpostId, Long behandlingId) {
        final List<UngdomsytelseSøktStartdato> søknadsperiodeliste = new ArrayList<>();
        startDatoer.stream()
            .map(it -> new UngdomsytelseSøktStartdato(it, journalpostId))
            .forEach(søknadsperiodeliste::add);

        ungdomsytelseStartdatoRepository.lagre(behandlingId, søknadsperiodeliste);
    }


    /** Utvider fagsakperiode i en eller begge ender fra fom og tom i oppgitt perioder dersom denne medfører en større periode.
     * @param fom Fom-dato fra søknad
     * @param behandling Ny behandling
     */
    public void oppdaterFagsakperiode(LocalDate fom, Behandling behandling) {
        var nyPeriodeForFagsak = fagsakperiodeUtleder.utledNyPeriodeForFagsak(behandling, fom);
        fagsakRepository.utvidPeriode(behandling.getFagsakId(), nyPeriodeForFagsak.getFomDato(), nyPeriodeForFagsak.getTomDato());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }
}
