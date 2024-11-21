package no.nav.ung.sak.domene.iverksett;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;


@FagsakYtelseTypeRef
@ApplicationScoped
public class OpprettProsessTaskIverksettImpl extends OpprettProsessTaskIverksettTilkjentYtelseFelles {

    OpprettProsessTaskIverksettImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettImpl(FagsakProsessTaskRepository prosessTaskRepository,
                                           OppgaveTjeneste oppgaveTjeneste,
                                           InfotrygdFeedService infotrygdFeedService,
                                           StønadstatistikkService stønadstatistikkService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService, stønadstatistikkService);
    }
}
