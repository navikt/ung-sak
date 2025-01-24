package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class StartpunktUtlederStartdatoerTest {

    @Inject
    private EntityManager em;
    private UngdomsytelseStartdatoRepository søknadsperiodeRepository;
    private BehandlingRepository behandlingRepository;
    private StartpunktUtlederStartdatoer startpunktUtleder;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        søknadsperiodeRepository = new UngdomsytelseStartdatoRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        startpunktUtleder = new StartpunktUtlederStartdatoer(søknadsperiodeRepository);
        var fom = LocalDate.now();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(64));
        em.persist(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_ikke_utlede_startpunkt_uten_endringer() {
        // Arrange
        final var gammelGrunnlagId = lagGrunnlagMedJournalpostIder(1, 2);
        final var nyGrunnlagId = lagGrunnlagMedJournalpostIder(1, 2);

        // act
        final var startpunktType = startpunktUtleder.utledStartpunkt(null, gammelGrunnlagId, nyGrunnlagId);

        // assert
        assertThat(startpunktType).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_utlede_startpunkt_lik_INIT_PERIODER_for_ulike_journalposter() {
        // Arrange
        final var gammelGrunnlagId = lagGrunnlagMedJournalpostIder(1);
        final var nyGrunnlagId = lagGrunnlagMedJournalpostIder(2);

        // act
        final var startpunktType = startpunktUtleder.utledStartpunkt(null, nyGrunnlagId, gammelGrunnlagId);

        // assert
        assertThat(startpunktType).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    @Test
    void skal_utlede_startpunkt_lik_INIT_PERIODER_for_nye_journalposter() {
        // Arrange
        final var gammelGrunnlagId = lagGrunnlagMedJournalpostIder(1);
        final var nyGrunnlagId = lagGrunnlagMedJournalpostIder(1, 2);

        // act
        final var startpunktType = startpunktUtleder.utledStartpunkt(null, nyGrunnlagId, gammelGrunnlagId);

        // assert
        assertThat(startpunktType).isEqualTo(StartpunktType.INIT_PERIODER);
    }

    private Long lagGrunnlagMedJournalpostIder(int... journalpostIder) {
        var søknadsperioder = Arrays.stream(journalpostIder).mapToObj(it ->
                new UngdomsytelseSøktStartdato(LocalDate.now().plusDays(it), new JournalpostId(Integer.toUnsignedLong(it))))
            .toList();
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        return søknadsperiodeRepository.hentGrunnlag(behandling.getId()).get().getId();
    }
}
