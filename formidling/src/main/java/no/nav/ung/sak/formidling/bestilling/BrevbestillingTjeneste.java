package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BehandlingBrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevMottaker;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE;
import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_ID_PARAM;

@Dependent
public class BrevbestillingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevbestillingTjeneste.class);

    private final BrevbestillingRepository brevbestillingRepository;
    private final DokArkivKlient dokArkivKlient;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public BrevbestillingTjeneste(BrevbestillingRepository brevbestillingRepository, DokArkivKlient dokArkivKlient, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokArkivKlient = dokArkivKlient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public BrevbestillingResultat bestillBrev(Behandling behandling, GenerertBrev generertBrev) {
        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer().getVerdi();

        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
            saksnummer,
            generertBrev.malType(),
            new BrevMottaker(generertBrev.mottaker().aktørId().getAktørId(), IdType.AKTØRID));

        var behandlingBestilling = new BehandlingBrevbestillingEntitet(
            behandling.getId(),
            generertBrev.malType().isVedtaksbrevmal(),
            bestilling
        );

        LOG.info("Brevbestilling forespurt {}", behandlingBestilling);

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
            DistribuerJournalpostRequest.DistribusjonsType.VEDTAK.name() : DistribuerJournalpostRequest.DistribusjonsType.VIKTIG.name());
        prosessTaskTjeneste.lagre(distTask);
        distTask.setCallIdFraEksisterende();

        LOG.info("Brevbestilling journalført med journalpostId={}", bestilling.getJournalpostId());
        return new BrevbestillingResultat(bestilling.getJournalpostId());
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
            .tema(OmrådeTema.UNG.getKode())
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
            case ENDRING_DOK -> "Endring";
            case OPPHØR_DOK -> "Opphør";
            case AVSLAG__DOK -> "Avslag";
            case MANUELT_VEDTAK_DOK -> "Fritekstvedtak";
            case GENERELT_FRITEKSTBREV -> "Fritekst generelt brev";
        };
        return prefix + fraMal;
    }
}
