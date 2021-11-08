package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;

@ApplicationScoped
public class StønadstatistikkService {

    private Instance<StønadstatistikkHendelseBygger> stønadstatistikkService;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;
    
    
    public StønadstatistikkService(@Any Instance<StønadstatistikkHendelseBygger> stønadstatistikkService,
            BehandlingRepository behandlingRepository, ProsessTaskRepository prosessTaskRepository) {
        this.stønadstatistikkService = stønadstatistikkService;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }
    
    
    public void publiserHendelse(Behandling behandling) {
        final ProsessTaskData pd = PubliserStønadstatistikkHendelseTask.createProsessTaskData(behandling);
        prosessTaskRepository.lagre(pd);
    }
    
    
    StønadstatistikkHendelse lagHendelse(Long behandlingId) {
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final StønadstatistikkHendelseBygger bygger = forYtelse(behandling.getFagsakYtelseType()).orElseThrow();
        return bygger.lagHendelse(behandling.getUuid());
    }
    
    private Optional<StønadstatistikkHendelseBygger> forYtelse(FagsakYtelseType type) {
        return FagsakYtelseTypeRef.Lookup.find(stønadstatistikkService, type);
    }
}
