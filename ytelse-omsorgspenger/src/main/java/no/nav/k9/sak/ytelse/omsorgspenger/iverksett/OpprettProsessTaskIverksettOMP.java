package no.nav.k9.sak.ytelse.omsorgspenger.iverksett;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettTilkjentYtelseFelles;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OpprettProsessTaskIverksettOMP extends OpprettProsessTaskIverksettTilkjentYtelseFelles {

    private ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;

    OpprettProsessTaskIverksettOMP() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettOMP(FagsakProsessTaskRepository prosessTaskRepository,
                                          OppgaveTjeneste oppgaveTjeneste,
                                          InfotrygdFeedService infotrygdFeedService,
                                          ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste,
                                          StønadstatistikkService stønadstatistikkService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService, stønadstatistikkService);
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;

    }

    @Override
    public Optional<ProsessTaskData> opprettYtelsesSpesifikkeTasks(Behandling behandling) {
        return årskvantumDeaktiveringTjeneste.meldFraDersomDeaktivering(behandling);
    }
}
