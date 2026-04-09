package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.inject.Inject;
import no.nav.k9.felles.jpa.TomtResultatException;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CdiDbAwareTest
class OppgittForutgåendeMedlemskapRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private OppgittForutgåendeMedlemskapRepository repository;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    @Test
    void skal_lagre_og_hente_grunnlag_med_bosteder() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);
        var bosteder = Set.of(
            new OppgittBosted(LocalDate.of(2019, 7, 1), LocalDate.of(2022, 12, 31), "SWE"),
            new OppgittBosted(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 30), "DEU")
        );

        repository.lagre(behandling.getId(), fom, tom, bosteder);

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(grunnlag.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(grunnlag.getBosteder()).hasSize(2);
        assertThat(grunnlag.isAktiv()).isTrue();
    }

    @Test
    void skal_lagre_grunnlag_uten_bosteder() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);

        repository.lagre(behandling.getId(), fom, tom, Set.of());

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag.getBosteder()).isEmpty();
    }

    @Test
    void skal_deaktivere_eksisterende_grunnlag_ved_ny_lagring() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);
        var bosteder1 = Set.of(new OppgittBosted(fom, tom, "SWE"));
        var bosteder2 = Set.of(new OppgittBosted(fom, tom, "FIN"));

        repository.lagre(behandling.getId(), fom, tom, bosteder1);
        var førstGrunnlag = repository.hentGrunnlag(behandling.getId());

        repository.lagre(behandling.getId(), fom, tom, bosteder2);
        var nyttGrunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(nyttGrunnlag.getBosteder()).hasSize(1);
        assertThat(nyttGrunnlag.getBosteder().iterator().next().getLandkode()).isEqualTo("FIN");
        assertThat(nyttGrunnlag.getId()).isNotEqualTo(førstGrunnlag.getId());
    }

    @Test
    void skal_feile_når_ingen_grunnlag_eksisterer() {
        assertThatThrownBy(() -> repository.hentGrunnlag(behandling.getId()))
            .isInstanceOf(TomtResultatException.class);
    }

    @Test
    void skal_kopiere_grunnlag_til_ny_behandling() {
        var fom = LocalDate.of(2019, 7, 1);
        var tom = LocalDate.of(2024, 6, 30);
        var bosteder = Set.of(
            new OppgittBosted(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 6, 30), "DEU")
        );
        repository.lagre(behandling.getId(), fom, tom, bosteder);

        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        var kopiert = repository.hentGrunnlag(nyBehandling.getId());

        assertThat(kopiert.getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(kopiert.getPeriode().getTomDato()).isEqualTo(tom);
        assertThat(kopiert.getBosteder()).hasSize(1);
        assertThat(kopiert.getBosteder().iterator().next().getLandkode()).isEqualTo("DEU");
    }

    @Test
    void skal_ikke_kopiere_når_ingen_grunnlag_eksisterer() {
        Behandling nyBehandling = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(nyBehandling, new BehandlingLås(null));

        repository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), nyBehandling.getId());

        assertThat(repository.hentGrunnlagHvisEksisterer(nyBehandling.getId())).isEmpty();
    }
}
