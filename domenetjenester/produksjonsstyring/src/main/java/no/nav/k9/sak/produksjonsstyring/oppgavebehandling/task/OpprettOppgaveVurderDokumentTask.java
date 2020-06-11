package no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task;

import static no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask.TASKTYPE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveVurderDokumentTask extends FagsakProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveVurderDokument";
    public static final String KEY_BEHANDLENDE_ENHET = "behandlendEnhetsId";
    public static final String KEY_DOKUMENT_TYPE = "dokumentTypeId";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveVurderDokumentTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveVurderDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveVurderDokumentTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String behandlendeEnhet = prosessTaskData.getPropertyValue(KEY_BEHANDLENDE_ENHET);
        DokumentTypeId dokumentGruppe = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_DOKUMENT_TYPE))
            .map(DokumentTypeId::fraKode).orElse(DokumentTypeId.UDEFINERT);
        String beskrivelse = dokumentGruppe.getNavn();
        if (beskrivelse == null) {
            beskrivelse = dokumentGruppe.getKode();
        }

        String oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(prosessTaskData.getFagsakId(),
            OppgaveÅrsak.VURDER_DOKUMENT, behandlendeEnhet, "VL: " + beskrivelse, false);
        log.info("Oppgave opprettet i GSAK for å vurdere dokument på enhet. Oppgavenummer: {}", oppgaveId);
    }
}
