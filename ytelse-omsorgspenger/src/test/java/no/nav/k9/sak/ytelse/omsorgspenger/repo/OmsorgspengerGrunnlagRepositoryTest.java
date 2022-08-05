package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.kalkulus.typer.OrgNummer;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OmsorgspengerGrunnlagRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private OmsorgspengerGrunnlagRepository sutRepo;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingLåsRepository behandlingLåsRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void test_lagreOgFlushOppgittFraværFraSøknad() throws Exception {
        // Arrange
        Long behandlingId = startBehandling();

        // Act
        sutRepo.lagreOgFlushOppgittFraværFraSøknad(behandlingId, Set.of(oppgittFraværPeriode()));

        entityManager.clear();

        // Assert
        var maksPeriode = sutRepo.hentMaksPeriode(behandlingId);
        assertThat(maksPeriode).isNotNull();
    }

    @Test
    public void test_lagreOgFlushFraværskorrigeringerIm() throws Exception {
        // Arrange
        Long behandlingId = startBehandling();

        // Act
        sutRepo.lagreOgFlushFraværskorrigeringerIm(behandlingId, Set.of(oppgittFraværPeriode()));

        entityManager.clear();

        // Assert
        var maksPeriode = sutRepo.hentMaksPeriode(behandlingId);
        assertThat(maksPeriode).isNotNull();
    }

    @Test
    public void test_lagreOgFlushSammenslåttOppgittFravær() throws Exception {
        // Arrange
        Long behandlingId = startBehandling();

        // Act

        sutRepo.lagreOgFlushSammenslåttOppgittFravær(behandlingId, new OppgittFravær(Set.of(oppgittFraværPeriode())));

        entityManager.clear();

        // Assert
        var maksPeriode = sutRepo.hentMaksPeriode(behandlingId);
        assertThat(maksPeriode).isNotNull();
    }

    private OppgittFraværPeriode oppgittFraværPeriode() {
        return new OppgittFraværPeriode(
            new JournalpostId(99L),
            LocalDate.now(), LocalDate.now().plusDays(1),
            UttakArbeidType.ARBEIDSTAKER,
            Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG),
            InternArbeidsforholdRef.nyRef(),
            Duration.ofHours(5),
            FraværÅrsak.ORDINÆRT_FRAVÆR,
            SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
    }

    private Long startBehandling() {
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.OMSORGSPENGER, BehandlingStatus.UTREDES);

        Long behandlingId = behandling.getId();
        behandlingRepository.lagreOgClear(behandling, behandlingLåsRepository.taLås(behandlingId));

        var saksnummer = behandling.getFagsak().getSaksnummer();
        assertThat(saksnummer).isNotNull();
        return behandlingId;
    }

}
