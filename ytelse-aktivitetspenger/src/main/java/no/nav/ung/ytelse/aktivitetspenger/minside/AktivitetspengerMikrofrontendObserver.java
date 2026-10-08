package no.nav.ung.ytelse.aktivitetspenger.minside;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakStatusEvent;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aktiverer Min side-mikrofrontenden for aktivitetspenger når en ny aktivitetspenger-fagsak opprettes.
 */
@ApplicationScoped
public class AktivitetspengerMikrofrontendObserver {

    private static final Logger log = LoggerFactory.getLogger(AktivitetspengerMikrofrontendObserver.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakRepository fagsakRepository;
    private boolean mikrofrontendEnabled;

    AktivitetspengerMikrofrontendObserver() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerMikrofrontendObserver(ProsessTaskTjeneste prosessTaskTjeneste,
                                                 FagsakRepository fagsakRepository,
                                                 @KonfigVerdi(value = "AKTIVITETSPENGER_MIKROFRONTEND_ENABLED", required = false, defaultVerdi = "false") boolean mikrofrontendEnabled) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.mikrofrontendEnabled = mikrofrontendEnabled;
    }

    public void observerFagsakOpprettet(@Observes FagsakStatusEvent event) {
        if (!mikrofrontendEnabled || !erNyAktivitetspengerFagsak(event)) {
            return;
        }

        var fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        if (fagsak.erIkkeDigitalBruker()) {
            log.info("Aktiverer ikke mikrofrontend for aktivitetspenger for ikke-digital bruker, saksnummer={}", fagsak.getSaksnummer());
            return;
        }

        var task = ProsessTaskData.forProsessTask(AktiverMikrofrontendBrukerdialogTask.class);
        task.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
    }

    private static boolean erNyAktivitetspengerFagsak(FagsakStatusEvent event) {
        return FagsakYtelseType.AKTIVITETSPENGER == event.getYtelseType()
            && event.getForrigeStatus() == null
            && FagsakStatus.OPPRETTET == event.getNyStatus();
    }
}
