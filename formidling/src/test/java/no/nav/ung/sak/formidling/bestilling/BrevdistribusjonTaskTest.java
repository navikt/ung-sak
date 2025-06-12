package no.nav.ung.sak.formidling.bestilling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeType;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.*;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevHistorikkinnslagTjeneste;
import no.nav.ung.sak.formidling.SafFake;
import no.nav.ung.sak.formidling.dokdist.DokDistRestKlientFake;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE;
import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_ID_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevdistribusjonTaskTest {
    @Inject
    private EntityManager entityManager;

    private DokDistRestKlientFake dokDistKlient;
    private BrevbestillingRepository brevbestillingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private final SafFake safTjeneste = new SafFake();

    @BeforeEach
    void setUp() {
        dokDistKlient = new DokDistRestKlientFake();
        brevbestillingRepository = new BrevbestillingRepository(entityManager);
        historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
    }

    @Test
    void skalDistribuere() {

        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
            "ABC",
            DokumentMalType.INNVILGELSE_DOK,
            new BrevMottaker("123", IdType.AKTØRID));

        var behandlingBrevbestilling = new BehandlingBrevbestillingEntitet(
            123L,
            true,
            bestilling
        );

        String jpId = "jp123";
        String dokumentId = "567";
        safTjeneste.leggTilJournalpost(new SafFake.JournalpostFake(jpId, dokumentId, DokumentMalType.INNVILGELSE_DOK));

        bestilling.generertOgJournalført(TemplateType.INNVILGELSE, jpId);
        brevbestillingRepository.lagre(bestilling);

        var pd = ProsessTaskData.forProsessTask(BrevdistribusjonTask.class);
        pd.setProperty(BREVBESTILLING_ID_PARAM, bestilling.getId().toString());
        pd.setProperty(BREVBESTILLING_DISTRIBUSJONSTYPE, DistribusjonsType.VEDTAK.name());

        var brevHistorikkinnslagTjeneste = new BrevHistorikkinnslagTjeneste(historikkinnslagRepository, safTjeneste);
        var task = new BrevdistribusjonTask(brevbestillingRepository, dokDistKlient, brevHistorikkinnslagTjeneste);
        task.doTask(pd);

        assertThat(dokDistKlient.getRequests()).hasSize(1);
        DistribuerJournalpostRequest req = dokDistKlient.getRequests().getFirst();
        assertThat(req.journalpostId()).isEqualTo(bestilling.getJournalpostId());
        assertThat(req.bestillendeFagsystem()).isEqualTo(Fagsystem.UNG_SAK.getOffisiellKode());
        assertThat(req.distribusjonstidspunkt()).isEqualTo("KJERNETID");
        assertThat(req.distribusjonstype()).isEqualTo(DistribusjonsType.VEDTAK);
        assertThat(req.dokumentProdApp()).isEqualTo("UNG_SAK");

        BrevbestillingEntitet oppdatertBestilling = brevbestillingRepository.hent(bestilling.getId());
        assertThat(oppdatertBestilling.getDokdistBestillingId()).isEqualTo(dokDistKlient.getResponses().getFirst().bestillingsId());
        assertThat(oppdatertBestilling.getStatus()).isEqualTo(BrevbestillingStatusType.FULLFØRT);

        List<Historikkinnslag> historikkinnslags = historikkinnslagRepository.hent(new Saksnummer(bestilling.getSaksnummer()));
        assertThat(historikkinnslags.size()).isEqualTo(1);
        Historikkinnslag historikkinnslag = historikkinnslags.getFirst();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);
        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandlingBrevbestilling.getBehandlingId());

        var dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(1);
        assertThat(dokumentLinker.getFirst().getDokumentId()).isEqualTo(dokumentId);
        assertThat(dokumentLinker.getFirst().getJournalpostId().getVerdi()).isEqualTo(jpId);
        assertThat(dokumentLinker.getFirst().getLinkTekst()).isEqualTo("Brev");

        assertThat(historikkinnslag.getTittel()).isEqualTo("Brev bestilt");

        var linjer = historikkinnslag.getLinjer();
        assertThat(linjer).hasSize(1);
        assertThat(linjer.getFirst().getType()).isEqualTo(HistorikkinnslagLinjeType.TEKST);
        assertThat(linjer.getFirst().getTekst()).isEqualTo(TemplateType.INNVILGELSE.getBeskrivelse());
    }

    @Test
    void skalFeileHvisIkkeJournalførtStatus() {
        //TODO
    }
}
