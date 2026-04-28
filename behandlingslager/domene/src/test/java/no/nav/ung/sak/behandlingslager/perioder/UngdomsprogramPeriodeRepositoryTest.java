package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsprogramPeriodeRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UngdomsprogramPeriodeRepository repository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

    @Test
    void skal_hente_grunnlag_basert_på_id() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreUngdomsprogramGrunnlag(LocalDate.now());

        // Act
        Optional<UngdomsprogramPeriodeGrunnlag> resultat = repository.hentGrunnlagBasertPåId(ungdomsprogramPeriodeGrunnlag.getId());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getId()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getId());
    }

    @Test
    void skal_finne_aktiv_grunnlag_id() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag = lagreUngdomsprogramGrunnlag(LocalDate.now());

        // Act
        var resultat = repository.finnAktivGrunnlagId(behandling.getId());

        // Assert
        assertThat(resultat.getGrunnlagRef()).isEqualTo(ungdomsprogramPeriodeGrunnlag.getId());
    }

    @Test
    void skal_finne_diff() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag1 = lagreUngdomsprogramGrunnlag(LocalDate.now());
        final var ungdomsprogramPeriodeGrunnlag2 = lagreUngdomsprogramGrunnlag(LocalDate.now().plusDays(1));

        // Act
        final var diffResult = repository.diffResultat(EndringsresultatDiff.medDiff(UngdomsprogramPeriodeGrunnlag.class, ungdomsprogramPeriodeGrunnlag1.getId(), ungdomsprogramPeriodeGrunnlag2.getId()), true);
        final var harDiff = !diffResult.isEmpty();

        // Assert
        assertThat(harDiff).isEqualTo(true);
    }

    @Test
    void skal_ikke_finne_diff() {
        // Arrange
        final var ungdomsprogramPeriodeGrunnlag1 = lagreUngdomsprogramGrunnlag(LocalDate.now());
        final var ungdomsprogramPeriodeGrunnlag2 = lagreUngdomsprogramGrunnlag(LocalDate.now());

        // Act
        final var diffResult = repository.diffResultat(EndringsresultatDiff.medDiff(UngdomsprogramPeriodeGrunnlag.class, ungdomsprogramPeriodeGrunnlag1.getId(), ungdomsprogramPeriodeGrunnlag2.getId()), true);
        final var harDiff = !diffResult.isEmpty();

        // Assert
        assertThat(harDiff).isEqualTo(false);
    }

    @Test
    void lagre_uten_harUtvidetKvote_bevarer_eksisterende_utvidet_kvote() {
        // Arrange: lagre med utvidet kvote = true
        repository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(LocalDate.now(), LocalDate.now())), true);
        assertThat(repository.hentGrunnlag(behandling.getId()).map(g -> g.isHarUtvidetKvote())).contains(true);

        // Act: lagre perioder uten å oppgi harUtvidetKvote
        repository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))));

        // Assert: utvidet kvote skal fortsatt være true
        var grunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().isHarUtvidetKvote()).isTrue();
    }

    @Test
    void lagre_uten_harUtvidetKvote_beholder_false_når_ikke_satt() {
        // Arrange: lagre uten utvidet kvote
        repository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(LocalDate.now(), LocalDate.now())));

        // Act: lagre igjen uten å oppgi harUtvidetKvote
        repository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))));

        // Assert: utvidet kvote skal fortsatt være false
        var grunnlag = repository.hentGrunnlag(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().isHarUtvidetKvote()).isFalse();
    }

    private UngdomsprogramPeriodeGrunnlag lagreUngdomsprogramGrunnlag(LocalDate dato) {
        final var ungdomsprogramPeriodeGrunnlag = new UngdomsprogramPeriodeGrunnlag(behandling.getId());
        ungdomsprogramPeriodeGrunnlag.leggTil(List.of(new UngdomsprogramPeriode(dato, dato)));
        repository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(dato, dato)));
        return repository.hentGrunnlag(behandling.getId()).get();
    }
}
