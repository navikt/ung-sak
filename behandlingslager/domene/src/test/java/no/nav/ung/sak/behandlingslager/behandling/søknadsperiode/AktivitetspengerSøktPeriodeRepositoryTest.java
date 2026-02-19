package no.nav.ung.sak.behandlingslager.behandling.søknadsperiode;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

@CdiDbAwareTest
class AktivitetspengerSøktPeriodeRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private AktivitetspengerSøktPeriodeRepository repository;

    @Test
    void skal_lagre_og_hente_periode() {
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet søktPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        LocalDateTime mottattTid = LocalDateTime.now();
        AktivitetspengerSøktPeriode søktPeriodeEntitet = new AktivitetspengerSøktPeriode(behandling.getId(), new JournalpostId("JP-1"), mottattTid, søktPeriode);
        repository.lagreNyPeriode(søktPeriodeEntitet);
        Assertions.assertThat(repository.hentSøktePerioder(behandling.getId())).containsOnly(søktPeriodeEntitet);
    }


    Behandling opprettBehandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("FOO"), LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1));
        fagsakRepository.opprettNy(fagsak);
        Behandling behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        Long behandlingId = behandlingRepository.lagre(behandling, new BehandlingLås(null));
        return behandlingRepository.hentBehandling(behandlingId);
    }
}
