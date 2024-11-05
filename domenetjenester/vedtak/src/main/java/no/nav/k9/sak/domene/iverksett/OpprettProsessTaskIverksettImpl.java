package no.nav.k9.sak.domene.iverksett;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;


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
                                           StønadstatistikkService stønadstatistikkService,
                                           @KonfigVerdi(value = "SEND_INNTEKTSMELDING_FORESPORSEL", defaultVerdi = "false") boolean skalSendeForesporsel,
                                           @KonfigVerdi(value = "FJERN_INAKTIVE_VILKAR_RESULTAT", defaultVerdi = "true") boolean skalFjerneInaktiveVilkårsresultat) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService, stønadstatistikkService, skalSendeForesporsel, skalFjerneInaktiveVilkårsresultat);
    }
}
