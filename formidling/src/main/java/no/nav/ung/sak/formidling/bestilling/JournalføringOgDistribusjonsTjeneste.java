package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevMottaker;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequestBuilder;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE;
import static no.nav.ung.sak.formidling.bestilling.BrevdistribusjonTask.BREVBESTILLING_ID_PARAM;

@Dependent
public class JournalføringOgDistribusjonsTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(JournalføringOgDistribusjonsTjeneste.class);

    private final BrevbestillingRepository brevbestillingRepository;
    private final DokArkivKlient dokArkivKlient;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public JournalføringOgDistribusjonsTjeneste(BrevbestillingRepository brevbestillingRepository, DokArkivKlient dokArkivKlient, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokArkivKlient = dokArkivKlient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    /**
     * Journalfører og lager distribusjonstask.
     */
    public BrevbestillingResultat journalførOgDistribuer(Behandling behandling, BrevbestillingEntitet bestilling, GenerertBrev generertBrev) {
        ProsessTaskData distribusjonstaskMal = ProsessTaskData.forProsessTask(BrevdistribusjonTask.class);
        return journalførOgDistribuer(behandling, bestilling, generertBrev, distribusjonstaskMal);
    }

    /**
     *
     * Sørger for at distribuert etter evt. tidligere brev i samme fagsak.
     */
    public BrevbestillingResultat journalførOgDistribuerISekvens(Behandling behandling, BrevbestillingEntitet bestilling, GenerertBrev generertBrev) {
        ProsessTaskData distTaskMal = BrevbestillingTaskGenerator.formidlingProsessTaskIGruppe(BrevdistribusjonTask.class, behandling.getFagsakId());
        return journalførOgDistribuer(behandling, bestilling, generertBrev, distTaskMal);
    }

    private BrevbestillingResultat journalførOgDistribuer(Behandling behandling, BrevbestillingEntitet bestilling, GenerertBrev generertBrev, ProsessTaskData distTaskMal) {
        var dokArkivRequest = opprettJournalpostRequest(bestilling.getBrevbestillingUuid(), generertBrev, behandling);
        var opprettJournalpostResponse = dokArkivKlient.opprettJournalpost(dokArkivRequest);

        bestilling.journalført(
            opprettJournalpostResponse.journalpostId(),
            generertBrev.templateType(),
            new BrevMottaker(generertBrev.mottaker().aktørId().getAktørId(), IdType.AKTØRID));

        brevbestillingRepository.lagre(bestilling);
        distTaskMal.setBehandling(bestilling.getFagsakId(), bestilling.getBehandlingId());
        distTaskMal.setSaksnummer(behandling.getFagsak().getSaksnummer().toString());
        distTaskMal.setProperty(BREVBESTILLING_ID_PARAM, bestilling.getId().toString());
        distTaskMal.setProperty(BREVBESTILLING_DISTRIBUSJONSTYPE, bestilling.isVedtaksbrev() ?
            DistribuerJournalpostRequest.DistribusjonsType.VEDTAK.name() : DistribuerJournalpostRequest.DistribusjonsType.VIKTIG.name());
        prosessTaskTjeneste.lagre(distTaskMal);
        distTaskMal.setCallIdFraEksisterende();

        LOG.info("Brevbestilling med id={} journalført med journalpostId={}", bestilling.getId(), bestilling.getJournalpostId());
        return new BrevbestillingResultat(new JournalpostId(bestilling.getJournalpostId()));
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
        var prefix = "Ungdomsprogramytelse ";
        String fraMal = switch (dokumentMalType) {
            case HENLEGG_BEHANDLING_DOK -> "Henleggelse";
            case INNVILGELSE_DOK -> "Innvilgelse";
            case ENDRING_BARNETILLEGG -> "Endring Barnetillegg";
            case ENDRING_PROGRAMPERIODE -> "Endring Programperiode";
            case ENDRING_INNTEKT -> "Endring Inntekt";
            case ENDRING_HØY_SATS -> "Endring Høy Sats";
            case OPPHØR_DOK -> "Opphør";
            case AVSLAG__DOK -> "Avslag";
            case MANUELT_VEDTAK_DOK -> "Fritekstvedtak";
            case GENERELT_FRITEKSTBREV -> "Fritekst generelt brev";
        };
        return prefix + fraMal;
    }

}
