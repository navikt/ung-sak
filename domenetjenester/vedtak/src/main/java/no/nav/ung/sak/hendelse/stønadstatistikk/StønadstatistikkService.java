package no.nav.ung.sak.hendelse.stønadstatistikk;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;

@ApplicationScoped
public class StønadstatistikkService {

    private Instance<StønadstatistikkHendelseBygger> stønadstatistikkHendelseBygger;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskRepository;

    public StønadstatistikkService() {

    }

    @Inject
    public StønadstatistikkService(@Any Instance<StønadstatistikkHendelseBygger> stønadstatistikkHendelseBygger,
                                   BehandlingRepository behandlingRepository, ProsessTaskTjeneste prosessTaskRepository) {
        this.stønadstatistikkHendelseBygger = stønadstatistikkHendelseBygger;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void publiserHendelse(Behandling behandling) {
        Set<FagsakYtelseType> aktiverteForYtelsetyper = Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN, FagsakYtelseType.OMP);

        if (!aktiverteForYtelsetyper.contains(behandling.getFagsakYtelseType())) {
            return;
        }
        final ProsessTaskData pd = PubliserStønadstatistikkHendelseTask.createProsessTaskData(behandling);
        prosessTaskRepository.lagre(pd);
    }


    public StønadstatistikkHendelse lagHendelse(Long behandlingId) {
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final StønadstatistikkHendelseBygger bygger = forYtelse(behandling.getFagsakYtelseType()).orElseThrow();
        return bygger.lagHendelse(behandling.getUuid());
    }

    private Optional<StønadstatistikkHendelseBygger> forYtelse(FagsakYtelseType type) {
        return FagsakYtelseTypeRef.Lookup.find(stønadstatistikkHendelseBygger, type);
    }
}
