package no.nav.k9.sak.domene.iverksett;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;


@FagsakYtelseTypeRef
@ApplicationScoped
public class OpprettProsessTaskIverksettImpl extends OpprettProsessTaskIverksettFelles {

    OpprettProsessTaskIverksettImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettImpl(ProsessTaskRepository prosessTaskRepository,
                                         OppgaveTjeneste oppgaveTjeneste,
                                         InfotrygdFeedService infotrygdFeedService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
    }
}
