package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
public class UttalelseRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UttalelseRepository repository;
    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        behandling = lagreNyBehandling();
    }

    private Behandling lagreNyBehandling() {
        var ny = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(ny, behandlingRepository.taSkriveLås(ny.getId()));
        return ny;
    }


    @Test
    void skal_hente_grunnlag_basert_på_behandling_id() {
        // Arrange
        lagreUttalelseGrunnlag(behandling.getId());

        // Act
        Optional<UttalelseGrunnlag> resultat = repository.hentEksisterendeGrunnlag(behandling.getId());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getId()).isNotNull();
        assertThat(resultat.get().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    void erstatter_bare_uttalelser_av_gitte_typer_og_beholder_andre() {
        var fra = lagreNyBehandling();
        var inntektFra = uttalelse(EndringType.ENDRET_INNTEKT, 1L);
        repository.lagre(fra.getId(), inntektFra);
        repository.lagre(fra.getId(), uttalelse(EndringType.ENDRET_PERIODE, 2L));
        var startdatoTil = uttalelse(EndringType.ENDRET_STARTDATO, 4L);
        repository.lagre(behandling.getId(), uttalelse(EndringType.ENDRET_INNTEKT, 3L));
        repository.lagre(behandling.getId(), startdatoTil);

        repository.erstattUttalelserMedKopiFra(fra.getId(), behandling.getId(), Set.of(EndringType.ENDRET_INNTEKT));

        assertThat(repository.hentUttalelser(behandling.getId(), EndringType.values()))
            .containsExactlyInAnyOrder(inntektFra, startdatoTil);
        assertThat(repository.hentUttalelser(fra.getId(), EndringType.values())).hasSize(2);
    }

    @Test
    void erstatting_deaktiverer_grunnlaget_når_ingen_uttalelser_er_igjen() {
        var fraUtenUttalelser = lagreNyBehandling();
        repository.lagre(behandling.getId(), uttalelse(EndringType.ENDRET_INNTEKT, 3L));

        repository.erstattUttalelserMedKopiFra(fraUtenUttalelser.getId(), behandling.getId(), Set.of(EndringType.ENDRET_INNTEKT));

        assertThat(repository.hentEksisterendeGrunnlag(behandling.getId())).isEmpty();
    }

    private static UttalelseV2 uttalelse(EndringType type, long journalpostId) {
        return new UttalelseV2(true, "Begrunnelse " + journalpostId,
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31)),
            new JournalpostId(journalpostId), type, UUID.randomUUID());
    }


    private void lagreUttalelseGrunnlag(Long behandlingId) {
        var uttalelse1 = new UttalelseV2(
            true,
            "Begrunnelse 1",
            DatoIntervallEntitet.fraOgMedTilOgMed(
                LocalDate.of(2025,8,1),
                LocalDate.of(2025,8,31)),
            new JournalpostId(1245L),
            EndringType.ENDRET_INNTEKT,
            UUID.randomUUID()
            );
        repository.lagre(behandlingId, uttalelse1);
    }
}
