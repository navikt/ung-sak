package no.nav.ung.sak.formidling.bestilling;

import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE;
import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_ID_PARAM;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.BrevGenerererTjeneste;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;

/**
 * <a href="https://confluence.adeo.no/pages/viewpage.action?pageId=377701645">dokarkiv doc</a>
 * <p>
 * <a href="https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/">dokarkiv-q2.dev swagger</a>
 */
@ApplicationScoped
@ProsessTask(value = BrevbestillingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BrevbestillingTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "formidling.brevbestilling";

    private static final Logger LOG = LoggerFactory.getLogger(BrevbestillingTask.class);

    private BehandlingRepository behandlingRepository;
    private BrevGenerererTjeneste brevGenerererTjeneste;
    private BrevbestillingRepository brevbestillingRepository;
    private DokArkivKlient dokArkivKlient;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public BrevbestillingTask(
            BehandlingRepository behandlingRepository,
            BrevGenerererTjeneste brevGenerererTjeneste,
            BrevbestillingRepository brevbestillingRepository,
            DokArkivKlient dokArkivKlient,
            ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.brevGenerererTjeneste = brevGenerererTjeneste;
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokArkivKlient = dokArkivKlient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    BrevbestillingTask() {
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData)  {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());

        if (behandling.getBehandlingResultatType() != BehandlingResultatType.INNVILGET) {
            LOG.warn("Dropper bestilling av brev da kun innvilgelse er støttet foreløpig");
            return;
        }

        validerBrevbestillingForespørsel(behandling);

        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer().getVerdi();

        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
                saksnummer,
                DokumentMalType.INNVILGELSE_DOK,
                new BrevMottaker(behandling.getAktørId().getAktørId(), IdType.AKTØRID));

        var behandlingBestilling = new BehandlingBrevbestillingEntitet(
                behandling.getId(),
                true,
                bestilling
        );

        LOG.info("Brevbestilling forespurt {}", behandlingBestilling);

        var generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId());

        brevbestillingRepository.lagreForBehandling(behandlingBestilling);

        var dokArkivRequest = opprettJournalpostRequest(bestilling.getBrevbestillingUuid(), generertBrev, behandling);
        var opprettJournalpostResponse = dokArkivKlient.opprettJournalpost(dokArkivRequest);
        //TODO vurder å putte templateType ved new'ing  istedenfor her...
        bestilling.generertOgJournalført(generertBrev.templateType(), opprettJournalpostResponse.journalpostId());

        brevbestillingRepository.lagreForBehandling(behandlingBestilling);
        var distTask = ProsessTaskData.forProsessTask(BrevdistribusjonTask.class);
        distTask.setBehandling(fagsak.getId(), behandling.getId());
        distTask.setSaksnummer(fagsak.getSaksnummer().getVerdi());
        distTask.setProperty(BREVBESTILLING_ID_PARAM, bestilling.getId().toString());
        distTask.setProperty(BREVBESTILLING_DISTRIBUSJONSTYPE, behandlingBestilling.isVedtaksbrev() ?
            DistribusjonsType.VEDTAK.name() : DistribusjonsType.VIKTIG.name());
        prosessTaskTjeneste.lagre(distTask);
        distTask.setCallIdFraEksisterende();

        LOG.info("Brevbestilling journalført med journalpostId={}", bestilling.getJournalpostId());

    }

    private void validerBrevbestillingForespørsel(Behandling behandling) {
        var tidligereBestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        var tidligereVedtaksbrev= tidligereBestillinger.stream().filter(BehandlingBrevbestillingEntitet::isVedtaksbrev).toList();
        if (!tidligereVedtaksbrev.isEmpty()) {
            String collect = tidligereVedtaksbrev.stream()
                    .map(BehandlingBrevbestillingEntitet::toString)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Det finnes allerede en bestilling for vedtaksbrev for behandling: " + collect);
        }
    }


    private OpprettJournalpostRequest opprettJournalpostRequest(UUID brevbestillingUuid, GenerertBrev generertBrev, Behandling behandling) {
        String tittel = utledTittel(generertBrev.malType());

        var avsenderMottaker = new OpprettJournalpostRequest.AvsenderMottaker(
            generertBrev.mottaker().fnr(),
            null,
            null,
            OpprettJournalpostRequest.AvsenderMottaker.IdType.FNR

        );
        var bruker = new OpprettJournalpostRequest.Bruker(
            generertBrev.mottaker().fnr(),
            OpprettJournalpostRequest.Bruker.BrukerIdType.FNR
        );

        var tilleggsopplysninger = new OpprettJournalpostRequest.Tilleggsopplysning(
            "ung.formidling.eRef",
            behandling.getUuid().toString());

        var sak = OpprettJournalpostRequest.Sak.forSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());

        var dokument = OpprettJournalpostRequest.Dokument.lagDokumentMedPdf(
            tittel,
            generertBrev.dokument().pdf(),
            generertBrev.malType().getKode());


        return new OpprettJournalpostRequestBuilder()
            .journalpostType("UTGAAENDE")
            .avsenderMottaker(avsenderMottaker)
            .bruker(bruker)
            .tema("OMS") //TODO endre for ung
            .behandlingstema("ab0271")
            .tittel(tittel)
            .kanal(null)
            .journalfoerendeEnhet("9999")
            .eksternReferanseId(brevbestillingUuid.toString())
            .tilleggsopplysninger(List.of(tilleggsopplysninger))
            .sak(sak)
            .dokumenter(List.of(dokument))
            .build();
    }

    private String utledTittel(DokumentMalType dokumentMalType) {
        var prefix = "Ungdomsytelse ";
        String fraMal = switch (dokumentMalType) {
            case HENLEGG_BEHANDLING_DOK -> "Henleggelse";
            case INNVILGELSE_DOK -> "Innvilgelse";
            case OPPHØR_DOK -> "Opphør";
            case AVSLAG__DOK -> "Avslag";
            case MANUELT_VEDTAK_DOK -> "Fritekstvedtak";
        };
        return prefix + fraMal;
    }
}
