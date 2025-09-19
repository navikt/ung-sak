package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UttalelseUtlederStartdatoerTest {

    @Inject
    private EntityManager em;
    private UttalelseRepository uttalelseRepository;
    private BehandlingRepository behandlingRepository;
    private StartpunktUtlederUttalelse startpunktUtleder;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        uttalelseRepository = new UttalelseRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        startpunktUtleder = new StartpunktUtlederUttalelse(uttalelseRepository);
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
    void skal_utlede_startpunkt_lik_VURDER_KOMPLETTHET_for_ulike_journalposter() {
        // Arrange
        final var gammelGrunnlagId = lagGrunnlagMedJournalpostIder(1);
        final var nyGrunnlagId = lagGrunnlagMedJournalpostIder(2);

        // act
        final var startpunktType = startpunktUtleder.utledStartpunkt(null, nyGrunnlagId, gammelGrunnlagId);

        // assert
        assertThat(startpunktType).isEqualTo(StartpunktType.INNHENT_REGISTEROPPLYSNINGER);
    }

    @Test
    void skal_utlede_startpunkt_lik_VURDER_KOMPLETTHET_for_nye_journalposter() {
        // Arrange
        final var gammelGrunnlagId = lagGrunnlagMedJournalpostIder(1);
        final var nyGrunnlagId = lagGrunnlagMedJournalpostIder(1, 2);

        // act
        final var startpunktType = startpunktUtleder.utledStartpunkt(null, nyGrunnlagId, gammelGrunnlagId);

        // assert
        assertThat(startpunktType).isEqualTo(StartpunktType.INNHENT_REGISTEROPPLYSNINGER);
    }

    private Long lagGrunnlagMedJournalpostIder(int... journalpostIder) {
        var utalelser = Arrays.stream(journalpostIder).mapToObj(it ->
                new UttalelseV2(
                    false,
                    "begrunnelse 1",
                    DatoIntervallEntitet.fraOgMedTilOgMed(
                        LocalDate.of(2025,8,1),
                        LocalDate.of(2025,8,31)),
                    new JournalpostId(Integer.toUnsignedLong(it)),
                    EndringType.ENDRET_INNTEKT,
                    UUID.randomUUID()
                    )).toList();
        uttalelseRepository.lagre(behandling.getId(), utalelser);
        return uttalelseRepository.hentEksisterendeGrunnlag(behandling.getId()).get().getId();
    }
}
