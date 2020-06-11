package no.nav.k9.sak.domene.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;


@FagsakYtelseTypeRef
@ApplicationScoped
public class OpprettProsessTaskIverksettImpl extends OpprettProsessTaskIverksettFelles {

    OpprettProsessTaskIverksettImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettImpl(FagsakProsessTaskRepository prosessTaskRepository,
                                           OppgaveTjeneste oppgaveTjeneste,
                                           InfotrygdFeedService infotrygdFeedService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
    }
}
