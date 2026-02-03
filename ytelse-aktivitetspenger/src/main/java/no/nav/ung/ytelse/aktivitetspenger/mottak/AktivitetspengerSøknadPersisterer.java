package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.AktivitetspengerFagsakperiodeUtleder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Dependent
public class AktivitetspengerSøknadPersisterer {

    private final SøknadRepository søknadRepository;
    private final FagsakRepository fagsakRepository;
    private final AktivitetspengerFagsakperiodeUtleder fagsakperiodeUtleder;
    private final AktivitetspengerSøktPeriodeRepository søktPeriodeRepository;


    @Inject
    public AktivitetspengerSøknadPersisterer(BehandlingRepositoryProvider repositoryProvider, FagsakRepository fagsakRepository,
                                             AktivitetspengerFagsakperiodeUtleder fagsakperiodeUtleder,
                                             AktivitetspengerSøktPeriodeRepository søktPeriodeRepository) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = fagsakRepository;
        this.fagsakperiodeUtleder = fagsakperiodeUtleder;
        this.søktPeriodeRepository = søktPeriodeRepository;
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

    public void lagreSøknadsperioder(no.nav.k9.søknad.felles.type.Periode periode, JournalpostId journalpostId, LocalDateTime mottattTid, Long behandlingId) {
        AktivitetspengerSøktPeriode søktPeriodeEntity = new AktivitetspengerSøktPeriode(behandlingId, journalpostId, mottattTid, DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()));
        søktPeriodeRepository.lagreNyPeriode(søktPeriodeEntity);
    }

    public void oppdaterFagsakperiode(Periode søknadsperiode, Behandling behandling) {
        var nyPeriodeForFagsak = fagsakperiodeUtleder.utledNyPeriodeForFagsak(behandling, søknadsperiode);
        fagsakRepository.utvidPeriode(behandling.getFagsakId(), nyPeriodeForFagsak.getFomDato(), nyPeriodeForFagsak.getTomDato());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }
}
