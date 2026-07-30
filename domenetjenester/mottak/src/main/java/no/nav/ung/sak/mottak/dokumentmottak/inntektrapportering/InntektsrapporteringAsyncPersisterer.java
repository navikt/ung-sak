package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class InntektsrapporteringAsyncPersisterer {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    public InntektsrapporteringAsyncPersisterer() {
    }

    @Inject
    public InntektsrapporteringAsyncPersisterer(MottatteDokumentRepository mottatteDokumentRepository,
                                                ProsessTaskTjeneste prosessTaskTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    public void opprettTaskForPersistering(Behandling behandling, MottattDokument dokument, OppgittInntekt inntekter) {
        try {
            var request = OppgittOpptjeningMapper.mapRequest(BehandlingReferanse.fra(behandling), dokument, inntekter);
            if (request.map(OppgittOpptjeningMottattRequest::getOppgittOpptjening).isEmpty()) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
            // Setter dokument til BEHANDLER for at AsyncAbakusLagreOpptjeningTask skal plukke den opp (brukes som idempotens-sjekk for kall mot abakus)
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = JsonObjectMapper.getMapper().writeValueAsString(request.get());
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, dokument.getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, dokument.getType().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();

            prosessTaskTjeneste.lagre(enkeltTask);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

}
