package no.nav.ung.sak.web.app.tjenester.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
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
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.FagsakProsessTaskKoblingDto;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.FjernFagsakProsessTaskKoblingerRequest;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.HentFagsakProsessTaskKoblingerRequest;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningFagsakProsessTaskRestTjeneste.SakMedForeldreløseKoblingerDto;
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
    void hent_skal_vise_foreldreløs_kobling_som_blokkerer_andre_tasker() {
        Long foreldreløsId = lagForeldreløsKobling(100L);
        ProsessTaskData vetoet = lagVetoetTask(foreldreløsId);
        Long utenSekvensId = lagForeldreløsKobling(null);

        List<FagsakProsessTaskKoblingDto> koblinger = hent();

        assertThat(koblinger).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(foreldreløsId, vetoet.getId(), utenSekvensId);
        var blokkerende = koblinger.get(0);
        assertThat(blokkerende.foreldreløs()).isTrue();
        assertThat(blokkerende.blokkererAndreTasker()).isTrue();
        assertThat(blokkerende.prosessTaskStatus()).isNull();
        var vetoetDto = koblinger.get(1);
        assertThat(vetoetDto.foreldreløs()).isFalse();
        assertThat(vetoetDto.blokkererAndreTasker()).isFalse();
        assertThat(vetoetDto.prosessTaskStatus()).isEqualTo(ProsessTaskStatus.VETO);
        assertThat(vetoetDto.blokkertAv()).isEqualTo(foreldreløsId);
        var utenSekvens = koblinger.get(2);
        assertThat(utenSekvens.foreldreløs()).isTrue();
        assertThat(utenSekvens.blokkererAndreTasker()).isFalse();
    }

    @Test
    void hent_skal_skrive_begrunnelse_til_diagnostikklogg() {
        lagForeldreløsKobling(100L);

        hent();

        assertThat(diagnostikklogg()).containsExactly(new LoggRad(ForvaltningFagsakProsessTaskRestTjeneste.HENT_PATH, BEGRUNNELSE.getTekst()));
    }

    @Test
    void hent_skal_gi_404_for_ukjent_saksnummer() {
        Response response = tjeneste.hentKoblinger(new HentFagsakProsessTaskKoblingerRequest(new Saksnummer("UKJENT"), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(diagnostikklogg()).isEmpty();
    }

    @Test
    void foreldreløse_skal_vise_foreldreløse_koblinger_for_alle_saker() {
        Long blokkerendeId = lagForeldreløsKobling(100L);
        lagVetoetTask(blokkerendeId);
        Fagsak annenSak = lagFagsak("SAK2");
        Long utenSekvensId = lagForeldreløsKobling(annenSak, null);
        Fagsak sakUtenForeldreløse = lagFagsak("SAK3");
        lagTask(sakUtenForeldreløse, ProsessTaskStatus.KLAR, 100L, null);

        List<SakMedForeldreløseKoblingerDto> saker = hentForeldreløse();

        assertThat(saker).extracting(SakMedForeldreløseKoblingerDto::saksnummer)
            .contains(SAKSNUMMER, annenSak.getSaksnummer())
            .doesNotContain(sakUtenForeldreløse.getSaksnummer());
        assertThat(finn(saker, SAKSNUMMER).foreldreløseKoblinger()).singleElement().satisfies(k -> {
            assertThat(k.prosessTaskId()).isEqualTo(blokkerendeId);
            assertThat(k.blokkererAndreTasker()).isTrue();
        });
        assertThat(finn(saker, annenSak.getSaksnummer()).foreldreløseKoblinger()).singleElement().satisfies(k -> {
            assertThat(k.prosessTaskId()).isEqualTo(utenSekvensId);
            assertThat(k.blokkererAndreTasker()).isFalse();
        });
        assertThat(diagnostikklogg()).isEmpty();
    }

    @Test
    void fjern_skal_fjerne_foreldreløs_kobling_frigi_veto_og_skrive_diagnostikklogg() {
        Long foreldreløsId = lagForeldreløsKobling(100L);
        ProsessTaskData vetoet = lagVetoetTask(foreldreløsId);

        Response response = tjeneste.fjernForeldreløseKoblinger(new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(foreldreløsId), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        var resultat = (FjernFagsakProsessTaskKoblingerResultat) response.getEntity();
        assertThat(resultat.fjernedeProsessTaskIder()).containsExactly(foreldreløsId);
        assertThat(resultat.frigittFraVeto()).isEqualTo(1);
        assertThat(diagnostikklogg()).singleElement().satisfies(rad -> {
            assertThat(rad.tjeneste()).isEqualTo(ForvaltningFagsakProsessTaskRestTjeneste.FJERN_PATH);
            assertThat(rad.begrunnelse()).contains(String.valueOf(foreldreløsId)).endsWith("Begrunnelse: " + BEGRUNNELSE.getTekst());
        });

        entityManager.clear();
        var koblinger = hent();
        assertThat(koblinger).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(vetoet.getId());
        assertThat(koblinger.getFirst().prosessTaskStatus()).isEqualTo(ProsessTaskStatus.KLAR);
    }

    @Test
    void fjern_skal_avvise_hele_forespørselen_når_en_prosesstask_finnes() {
        Long foreldreløsId = lagForeldreløsKobling(100L);
        ProsessTaskData vetoet = lagVetoetTask(foreldreløsId);

        Response response = tjeneste.fjernForeldreløseKoblinger(new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(foreldreløsId, vetoet.getId()), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat((String) response.getEntity()).contains(vetoet.getId() + " finnes (status VETO)");
        assertThat(diagnostikklogg()).isEmpty();
        assertThat(hent()).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(foreldreløsId, vetoet.getId());
    }

    @Test
    void fjern_skal_avvise_prosesstask_uten_kobling_på_fagsaken() {
        Long foreldreløsId = lagForeldreløsKobling(100L);

        Response response = tjeneste.fjernForeldreløseKoblinger(new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(foreldreløsId, 999_999_999L), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat((String) response.getEntity()).contains("999999999 har ingen kobling på fagsaken");
        assertThat(hent()).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(foreldreløsId);
    }

    @Test
    void fjern_skal_avvise_duplikater() {
        Long foreldreløsId = lagForeldreløsKobling(100L);

        Response response = tjeneste.fjernForeldreløseKoblinger(new FjernFagsakProsessTaskKoblingerRequest(SAKSNUMMER, List.of(foreldreløsId, foreldreløsId), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(hent()).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(foreldreløsId);
    }

    @Test
    void fjern_skal_gi_404_for_ukjent_saksnummer() {
        Long foreldreløsId = lagForeldreløsKobling(100L);

        Response response = tjeneste.fjernForeldreløseKoblinger(new FjernFagsakProsessTaskKoblingerRequest(new Saksnummer("UKJENT"), List.of(foreldreløsId), BEGRUNNELSE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(hent()).extracting(FagsakProsessTaskKoblingDto::prosessTaskId).containsExactly(foreldreløsId);
    }

    @SuppressWarnings("unchecked")
    private List<FagsakProsessTaskKoblingDto> hent() {
        Response response = tjeneste.hentKoblinger(new HentFagsakProsessTaskKoblingerRequest(SAKSNUMMER, BEGRUNNELSE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return (List<FagsakProsessTaskKoblingDto>) response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private List<SakMedForeldreløseKoblingerDto> hentForeldreløse() {
        Response response = tjeneste.hentForeldreløseKoblinger();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return (List<SakMedForeldreløseKoblingerDto>) response.getEntity();
    }

    private static SakMedForeldreløseKoblingerDto finn(List<SakMedForeldreløseKoblingerDto> saker, Saksnummer saksnummer) {
        return saker.stream().filter(s -> s.saksnummer().equals(saksnummer)).findFirst().orElseThrow();
    }

    private record LoggRad(String tjeneste, String begrunnelse) {
    }

    @SuppressWarnings("unchecked")
    private List<LoggRad> diagnostikklogg() {
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

    private Long lagForeldreløsKobling(Long gruppeSekvensNr) {
        return lagForeldreløsKobling(fagsak, gruppeSekvensNr);
    }

    /** Kobling til en prosesstask som er slettet, slik ryddProsessTasks har etterlatt i prod. */
    private Long lagForeldreløsKobling(Fagsak sak, Long gruppeSekvensNr) {
        ProsessTaskData task = lagTask(sak, ProsessTaskStatus.KLAR, gruppeSekvensNr, null);
        entityManager.clear();
        entityManager.createNativeQuery("delete from prosess_task where id = :id").setParameter("id", task.getId()).executeUpdate();
        return task.getId();
    }
}
