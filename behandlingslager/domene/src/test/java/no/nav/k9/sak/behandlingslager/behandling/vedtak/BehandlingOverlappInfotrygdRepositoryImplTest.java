package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingOverlappInfotrygdRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BehandlingOverlappInfotrygdRepository behandlingOverlappInfotrygdRepository;
    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        behandlingOverlappInfotrygdRepository = new BehandlingOverlappInfotrygdRepository(entityManager);
    }

    @Test
    public void lagre() {
        // Arrange
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        ÅpenDatoIntervallEntitet periodeInfotrygd = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 12, 1), LocalDate.of(2019, 1, 1));
        ÅpenDatoIntervallEntitet periodeVL = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1));
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(periodeInfotrygd)
            .medPeriodeVL(periodeVL)
            .build();

        // Act
        Long id = behandlingOverlappInfotrygdRepository.lagre(behandlingOverlappInfotrygd);
        repository.clear();

        // Assert
        BehandlingOverlappInfotrygd hentet = repository.hent(BehandlingOverlappInfotrygd.class, id);
        assertThat(hentet.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(hentet.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer());
        assertThat(hentet.getPeriodeInfotrygd()).isEqualTo(periodeInfotrygd);
        assertThat(hentet.getPeriodeVL()).isEqualTo(periodeVL);

    }
}
