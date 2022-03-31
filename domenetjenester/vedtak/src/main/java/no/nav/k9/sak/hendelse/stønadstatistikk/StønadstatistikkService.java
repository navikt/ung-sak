package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;

@ApplicationScoped
public class StønadstatistikkService {

    private Instance<StønadstatistikkHendelseBygger> stønadstatistikkHendelseBygger;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskRepository;
    private boolean enableStønadstatistikk;

    public StønadstatistikkService() {

    }

    @Inject
    public StønadstatistikkService(@Any Instance<StønadstatistikkHendelseBygger> stønadstatistikkHendelseBygger,
            BehandlingRepository behandlingRepository, ProsessTaskTjeneste prosessTaskRepository,
            @KonfigVerdi(value = "ENABLE_STONADSTATISTIKK", defaultVerdi = "false") boolean enableStønadstatistikk) {
        this.stønadstatistikkHendelseBygger = stønadstatistikkHendelseBygger;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.enableStønadstatistikk = enableStønadstatistikk;
    }


    public void publiserHendelse(Behandling behandling) {
        if (!enableStønadstatistikk || behandling.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
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
