package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.mottak.dokumentmottak.*;
import no.nav.ung.sak.typer.JournalpostId;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@DokumentGruppeRef(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING_KODE)
public class DokumentMottakerInntektrapportering implements Dokumentmottaker {

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;


    public DokumentMottakerInntektrapportering() {
    }

    @Inject
    public DokumentMottakerInntektrapportering(SøknadParser søknadParser,
                                               MottatteDokumentRepository mottatteDokumentRepository,
                                               ProsessTaskTjeneste prosessTaskTjeneste,
                                               HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> mottattDokument, Behandling behandling) {
        var behandlingId = behandling.getId();
        for (MottattDokument dokument : mottattDokument) {
            var søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            if (søknad.getKildesystem().isPresent()) {
                dokument.setKildesystem(søknad.getKildesystem().get().getKode());
            }
            // Setter dokument til BEHANDLER for at AsyncAbakusLagreOpptjeningTask skal plukke den opp (brukes som idempotens-sjekk for kall mot abakus)
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), dokument.getJournalpostId());
            lagreOppgittOpptjeningFraSøknad(søknad, behandling, dokument);
        }
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknad(Søknad søknad, Behandling behandling, MottattDokument dokument) {
        try {
            var inntekter = ((Ungdomsytelse) søknad.getYtelse()).getInntekter();
            var request = OppgittOpptjeningMapper.mapRequest(BehandlingReferanse.fra(behandling), dokument, inntekter);
            if (request.map(OppgittOpptjeningMottattRequest::getOppgittOpptjening).isEmpty()) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
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


    private void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsakId, journalpostId);
    }

    @Override
    public List<Trigger> getTriggere(Collection<MottattDokument> mottattDokument) {
        return mottattDokument.stream().map(it ->          søknadParser.parseSøknad(it))
            .map(it -> ((Ungdomsytelse) it.getYtelse()).getInntekter().getMinMaksPeriode())
            .map(it -> new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()), BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT))
            .toList();
    }

}
