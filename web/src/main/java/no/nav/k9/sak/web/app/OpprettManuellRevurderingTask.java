package no.nav.k9.sak.web.app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterApplikasjonTjeneste;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(OpprettManuellRevurderingTask.TASKTYPE)
public class OpprettManuellRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettManuellRevurdering";

    private static final Logger logger = LoggerFactory.getLogger(OpprettManuellRevurderingTask.class);

    private BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    

    protected OpprettManuellRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettManuellRevurderingTask(BehandlingsoppretterApplikasjonTjeneste behandlingsoppretterApplikasjonTjeneste,
            BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
            FagsakTjeneste fagsakTjeneste) {
        this.behandlingsoppretterApplikasjonTjeneste = behandlingsoppretterApplikasjonTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        final String[] saksnumre = pd.getPayloadAsString().split("\\s");
        revurderAlleSomProsessFeil(saksnumre);
    }
    
    public void revurderAlleSomProsessFeil(String[] saksnumre) {
        for (String saksnummer : saksnumre) {
            try {
                revurder(new Saksnummer(saksnummer));
            } catch (VLException e) {
                logger.info("Kunne ikke opprette manuell revurdering for: {}", saksnummer);
            }
        }
    }
    
    public void revurder(Saksnummer saksnummer) {
        final Optional<Fagsak> funnetFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        final Fagsak fagsak = funnetFagsak.get();
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.RE_FEIL_PROSESSUELL;
        final Behandling behandling = behandlingsoppretterApplikasjonTjeneste.opprettRevurdering(fagsak, behandlingÅrsakType);
        behandlingsprosessTjeneste.asynkStartBehandlingsprosess(behandling);
    }
}
