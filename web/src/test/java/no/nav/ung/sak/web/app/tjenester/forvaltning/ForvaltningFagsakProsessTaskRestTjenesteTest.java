package no.nav.ung.sak.web.app.tjenester.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.k9.prosesstask.impl.TaskManager;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTask;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskKobling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.FjernFagsakProsessTaskKoblingerRequest;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.SakMedKoblingerUtenProsessTaskDto;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.FjernFagsakProsessTaskKoblingerResultat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningFagsakProsessTaskRestTjenesteTest {

    private static final Saksnummer SAKSNUMMER = new Saksnummer("SAK1");
    private static final KortTekst BEGRUNNELSE = new KortTekst("TSFF-3297");

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private ForvaltningFagsakProsessTaskRestTjeneste tjeneste;

    private Fagsak fagsak;

    @BeforeEach
    void setup() {
        fagsakRepository = new FagsakRepository(entityManager);
        var prosessTaskTjeneste = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        fagsakProsessTaskRepository = new FagsakProsessTaskRepository(entityManager, prosessTaskTjeneste, mock(TaskManager.class));
        tjeneste = new ForvaltningFagsakProsessTaskRestTjeneste(fagsakRepository, fagsakProsessTaskRepository, entityManager);

        fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).medSaksnummer(SAKSNUMMER).build();
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void uten_prosesstask_skal_vise_koblinger_uten_prosesstask_for_alle_saker() {
        Long blokkerendeId = lagKoblingUtenProsessTask(100L);
        lagVetoetTask(blokkerendeId);
        Fagsak annenSak = lagFagsak("SAK2");
        Long utenSekvensId = lagKoblingUtenProsessTask(annenSak, null);
        Fagsak sakMedProsessTask = lagFagsak("SAK3");
        lagTask(sakMedProsessTask, ProsessTaskStatus.KLAR, 100L, null);

        List<SakMedKoblingerUtenProsessTaskDto> saker = tjeneste.hentKoblingerUtenProsessTask();

        assertThat(saker).extracting(SakMedKoblingerUtenProsessTaskDto::saksnummer)
            .contains(SAKSNUMMER, annenSak.getSaksnummer())
            .doesNotContain(sakMedProsessTask.getSaksnummer());
        assertThat(finn(saker, SAKSNUMMER).koblingerUtenProsessTask()).singleElement().satisfies(k -> {
            assertThat(k.prosessTaskId()).isEqualTo(blokkerendeId);
            assertThat(k.behandlingId()).isEqualTo(fagsak.getId() + 1);
            assertThat(k.blokkererAndreTasker()).isTrue();
        });
        assertThat(finn(saker, annenSak.getSaksnummer()).koblingerUtenProsessTask()).singleElement().satisfies(k -> {
            assertThat(k.prosessTaskId()).isEqualTo(utenSekvensId);
            assertThat(k.blokkererAndreTasker()).isFalse();
        });
        assertThat(diagnostikklogg()).isEmpty();
    }

    @Test
    void fjern_skal_fjerne_kobling_uten_prosesstask_frigi_veto_og_skrive_diagnostikklogg() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        ProsessTaskData vetoet = lagVetoetTask(slettetTaskId);

        FjernFagsakProsessTaskKoblingerResultat resultat = tjeneste.fjernKoblingerUtenProsessTask(new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(slettetTaskId), BEGRUNNELSE));

        assertThat(resultat.fjernedeProsessTaskIder()).containsExactly(slettetTaskId);
        assertThat(resultat.antallTaskerFrigittFraVeto()).isEqualTo(1);
        assertThat(diagnostikklogg()).singleElement().satisfies(rad -> {
            assertThat(rad.tjeneste()).isEqualTo(ForvaltningFagsakProsessTaskRestTjeneste.FJERN_PATH);
            assertThat(rad.begrunnelse()).contains(String.valueOf(slettetTaskId)).endsWith("Begrunnelse: " + BEGRUNNELSE.getTekst());
        });

        entityManager.clear();
        var koblinger = koblinger();
        assertThat(koblinger).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(vetoet.getId());
        assertThat(koblinger.getFirst().prosessTaskStatus()).isEqualTo(ProsessTaskStatus.KLAR);
    }

    @Test
    void fjern_skal_avvise_hele_forespørselen_når_en_prosesstask_finnes() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        ProsessTaskData vetoet = lagVetoetTask(slettetTaskId);

        var request = new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(slettetTaskId, vetoet.getId()), BEGRUNNELSE);

        var feil = assertThrows(IllegalArgumentException.class, () -> tjeneste.fjernKoblingerUtenProsessTask(request));
        assertThat(feil.getMessage()).contains(vetoet.getId() + " finnes (status VETO)");
        assertThat(diagnostikklogg()).isEmpty();
        assertThat(koblinger()).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId, vetoet.getId());
    }

    @Test
    void fjern_skal_avvise_prosesstask_uten_kobling_på_fagsaken() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);

        var request = new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(slettetTaskId, 999_999_999L), BEGRUNNELSE);

        var feil = assertThrows(IllegalArgumentException.class, () -> tjeneste.fjernKoblingerUtenProsessTask(request));
        assertThat(feil.getMessage()).contains("999999999 har ingen kobling på fagsaken");
        assertThat(koblinger()).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId);
    }

    @Test
    void fjern_skal_avvise_duplikater() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);

        var request = new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(slettetTaskId, slettetTaskId), BEGRUNNELSE);

        assertThrows(IllegalArgumentException.class, () -> tjeneste.fjernKoblingerUtenProsessTask(request));
        assertThat(koblinger()).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId);
    }

    @Test
    void fjern_skal_feile_for_ukjent_saksnummer() {
        Long slettetTaskId = lagKoblingUtenProsessTask(100L);
        var request = new FjernFagsakProsessTaskKoblingerRequest(new Saksnummer("UKJENT"), List.of(slettetTaskId), BEGRUNNELSE);

        assertThrows(IllegalArgumentException.class, () -> tjeneste.fjernKoblingerUtenProsessTask(request));
        assertThat(koblinger()).extracting(FagsakProsessTaskKobling::prosessTaskId).containsExactly(slettetTaskId);
    }

    private List<FagsakProsessTaskKobling> koblinger() {
        return fagsakProsessTaskRepository.hentKoblinger(fagsak.getId());
    }

    private static SakMedKoblingerUtenProsessTaskDto finn(List<SakMedKoblingerUtenProsessTaskDto> saker, Saksnummer saksnummer) {
        return saker.stream().filter(s -> s.saksnummer().equals(saksnummer)).findFirst().orElseThrow();
    }

    private record LoggRad(String tjeneste, String begrunnelse) {
    }

    @SuppressWarnings("unchecked")
    private List<LoggRad> diagnostikklogg() {
        entityManager.flush();
        List<Object[]> rader = entityManager.createNativeQuery("select tjeneste, begrunnelse from diagnostikk_fagsak_logg where fagsak_id = :fagsakId order by id")
            .setParameter("fagsakId", fagsak.getId())
            .getResultList();
        return rader.stream().map(r -> new LoggRad((String) r[0], (String) r[1])).toList();
    }

    private ProsessTaskData lagVetoetTask(Long blokkertAv) {
        return lagTask(ProsessTaskStatus.VETO, 200L, blokkertAv);
    }

    private ProsessTaskData lagTask(ProsessTaskStatus status, Long gruppeSekvensNr, Long blokkertAv) {
        return lagTask(fagsak, status, gruppeSekvensNr, blokkertAv);
    }

    private ProsessTaskData lagTask(Fagsak sak, ProsessTaskStatus status, Long gruppeSekvensNr, Long blokkertAv) {
        Long sakensBehandlingId = sak.getId() + 1;
        ProsessTaskData task = ProsessTaskData.forTaskType(new TaskType("behandlingskontroll.opprettRevurderingEllerDiff"));
        task.setBehandling(sak.getId(), sakensBehandlingId);
        task.setStatus(status);
        task.setBlokkertAvProsessTaskId(blokkertAv);
        fagsakProsessTaskRepository.lagreNyGruppe(task);
        fagsakProsessTaskRepository.lagre(new FagsakProsessTask(sak.getId(), sakensBehandlingId.toString(), task.getId(), gruppeSekvensNr, task.getTaskType()));
        entityManager.flush();
        return task;
    }

    private Fagsak lagFagsak(String saksnummer) {
        Fagsak sak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).medSaksnummer(new Saksnummer(saksnummer)).build();
        fagsakRepository.opprettNy(sak);
        return sak;
    }

    private Long lagKoblingUtenProsessTask(Long gruppeSekvensNr) {
        return lagKoblingUtenProsessTask(fagsak, gruppeSekvensNr);
    }

    /** Kobling til en prosesstask som er slettet, slik ryddProsessTasks har etterlatt i prod. */
    private Long lagKoblingUtenProsessTask(Fagsak sak, Long gruppeSekvensNr) {
        ProsessTaskData task = lagTask(sak, ProsessTaskStatus.KLAR, gruppeSekvensNr, null);
        entityManager.clear();
        entityManager.createNativeQuery("delete from prosess_task where id = :id").setParameter("id", task.getId()).executeUpdate();
        return task.getId();
    }
}
