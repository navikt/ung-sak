package no.nav.k9.sak.ytelse.omsorgspenger.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettFelles;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjenesteImpl;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OpprettProsessTaskIverksettOMPImpl extends OpprettProsessTaskIverksettFelles {

    private ÅrskvantumDeaktiveringTjenesteImpl årskvantumDeaktiveringTjeneste;

    OpprettProsessTaskIverksettOMPImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettOMPImpl(ProsessTaskRepository prosessTaskRepository,
                                              OppgaveTjeneste oppgaveTjeneste,
                                              InfotrygdFeedService infotrygdFeedService,
                                              ÅrskvantumDeaktiveringTjenesteImpl årskvantumDeaktiveringTjeneste) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;

    }

    @Override
    public void meldIfraOmIverksettingTilÅrskvantum(Behandling behandling) {
        årskvantumDeaktiveringTjeneste.meldIfraOmIverksetting(behandling);
    }
}
