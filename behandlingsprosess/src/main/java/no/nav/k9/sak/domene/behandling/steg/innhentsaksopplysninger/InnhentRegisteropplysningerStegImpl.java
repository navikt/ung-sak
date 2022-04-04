package no.nav.k9.sak.domene.behandling.steg.innhentsaksopplysninger;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INNHENT_REGISTEROPP;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;

@BehandlingStegRef(stegtype = INNHENT_REGISTEROPP)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerStegImpl implements InnhentRegisteropplysningerSteg {

    private static final Logger log = LoggerFactory.getLogger(InnhentRegisteropplysningerStegImpl.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    InnhentRegisteropplysningerStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                               FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (harAltPlanlagtRegisterinnhenting(behandling)) {
            log.info("Har allerede planlagt registerinnhenting, setter steget på vent i påvente av dette skal forekomme");
        } else {
            behandlingProsesseringTjeneste.opprettTasksForInitiellRegisterInnhenting(behandling);
        }

        return BehandleStegResultat.settPåVent();
    }

    private boolean harAltPlanlagtRegisterinnhenting(Behandling behandling) {
        var planlagteTasker = fagsakProsessTaskRepository.sjekkStatusProsessTasks(behandling.getFagsakId(), behandling.getId(), null)
            .stream()
            .map(ProsessTaskData::getTaskType)
            .collect(Collectors.toSet());

        if (planlagteTasker.isEmpty()) {
            return false;
        }

        var forventedeTasks = behandlingProsesseringTjeneste.utledRegisterinnhentingTaskTyper(behandling);
        if (forventedeTasks.isEmpty()) {
            return false;
        }

        return planlagteTasker.containsAll(forventedeTasks);
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
