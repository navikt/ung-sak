package no.nav.k9.sak.innsyn.hendelse.republisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.innsyn.hendelse.InnsynEventTjeneste;
import no.nav.k9.sak.innsyn.hendelse.republisering.PubliserBehandlingEntitet.Status;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class RepubliserInnsynEventTaskTest {

    @Inject
    private EntityManager entityManager;

    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();
    private final InnsynEventTjeneste innsynEventTjeneste = mock();
    private RepubliserInnsynEventTask task;

    @BeforeEach
    void setup() {
        task = new RepubliserInnsynEventTask(prosessTaskTjeneste, innsynEventTjeneste, new PubliserBehandlingInnsynRepository(entityManager));
    }

    @Test
    void prosesserer_alle_og_stopper() {
        doNothing().when(innsynEventTjeneste).publiserBehandling(any());

        // lag 3 behandlinger
        var kjøreId = UUID.randomUUID();
        entityManager.persist(new PubliserBehandlingEntitet(1L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        entityManager.persist(new PubliserBehandlingEntitet(2L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        entityManager.persist(new PubliserBehandlingEntitet(3L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        entityManager.flush();

        // kjør task
        ProsessTaskData pd = ProsessTaskData.forProsessTask(RepubliserInnsynEventTask.class);
        pd.setProperty(RepubliserInnsynEventTask.KJØRING_ID_PROP, kjøreId.toString());
        pd.setProperty(RepubliserInnsynEventTask.ANTALL_PER_KJØRING_PROP, "1");

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.NY, Status.NY, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.NY, Status.FULLFØRT, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.FULLFØRT, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.FULLFØRT, Status.FULLFØRT);

        task.doTask(pd);

        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.FULLFØRT, Status.FULLFØRT);

        // verifiser prosesstask ikke lages etter siste
        verify(prosessTaskTjeneste, times(3)).lagre(any(ProsessTaskData.class));
    }


    @Test
    void hopper_over_feilede() {
        doNothing().when(innsynEventTjeneste).publiserBehandling(any());

        // lag 3 behandlinger
        var kjøreId = UUID.randomUUID();

        entityManager.persist(new PubliserBehandlingEntitet(1L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        doNothing().when(innsynEventTjeneste).publiserBehandling(1L);

        entityManager.persist(new PubliserBehandlingEntitet(2L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        doThrow(new IllegalStateException("feil fra test")).when(innsynEventTjeneste).publiserBehandling(2L);

        entityManager.persist(new PubliserBehandlingEntitet(3L, kjøreId, PubliserBehandlingEntitet.KjøringType.INNSYN));
        doNothing().when(innsynEventTjeneste).publiserBehandling(3L);

        entityManager.flush();


        // kjør task
        ProsessTaskData pd = ProsessTaskData.forProsessTask(RepubliserInnsynEventTask.class);
        pd.setProperty(RepubliserInnsynEventTask.KJØRING_ID_PROP, kjøreId.toString());
        pd.setProperty(RepubliserInnsynEventTask.ANTALL_PER_KJØRING_PROP, "1");

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.NY, Status.NY, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.NY, Status.FEILET, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.FEILET, Status.FULLFØRT);

        task.doTask(pd);
        assertThat(hentAlle(kjøreId)).extracting(PubliserBehandlingEntitet::getStatus)
            .containsExactlyInAnyOrder(Status.FULLFØRT, Status.FEILET, Status.FULLFØRT);


        // verifiser prosesstask ikke lages etter siste
        verify(prosessTaskTjeneste, times(3)).lagre(any(ProsessTaskData.class));



    }

    private List<PubliserBehandlingEntitet> hentAlle(UUID kjøreId) {
        return entityManager.createQuery("select p from PubliserBehandlingEntitet p where kjøringUuid = :k", PubliserBehandlingEntitet.class)
            .setParameter("k", kjøreId)
            .getResultList();
    }

}
