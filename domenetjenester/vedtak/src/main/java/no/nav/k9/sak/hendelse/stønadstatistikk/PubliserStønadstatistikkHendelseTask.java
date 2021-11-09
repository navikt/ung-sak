package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.PubliserInfotrygdFeedElementTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;

@ApplicationScoped
@ProsessTask(PubliserStønadstatistikkHendelseTask.TASKTYPE)
public class PubliserStønadstatistikkHendelseTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.publiserStonadstatistikk";
    private static final Logger logger = LoggerFactory.getLogger(PubliserStønadstatistikkHendelseTask.class);

    private StønadstatistikkService stønadstatistikkService;
    private StønadstatistikkHendelseMeldingProducer meldingProducer;

    
    public PubliserStønadstatistikkHendelseTask() {}

    @Inject
    public PubliserStønadstatistikkHendelseTask(StønadstatistikkService stønadstatistikkService,
            StønadstatistikkHendelseMeldingProducer meldingProducer) {
        this.stønadstatistikkService = stønadstatistikkService;
        this.meldingProducer = meldingProducer;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        final String key = Objects.requireNonNull(pd.getSaksnummer());
        final StønadstatistikkHendelse hendelse = stønadstatistikkService.lagHendelse(Long.parseLong(pd.getBehandlingId()));
        if (hendelse == null) {
            logger.info("Publiserer IKKE hendelse til stønadstatistikk. Key: '{}'", key);
            return;
        }
        
        final String value = StønadstatistikkSerializer.toJson(hendelse);
        
        logger.info("Publiserer hendelse til stønadstatistikk. Key: '{}'", key);

        meldingProducer.send(key, value);
    }
    
    
    public static ProsessTaskData createProsessTaskData(Behandling behandling) {
        final ProsessTaskData pd = new ProsessTaskData(PubliserStønadstatistikkHendelseTask.TASKTYPE);
        final Fagsak fagsak = behandling.getFagsak();
        final String saksnummer = fagsak.getSaksnummer().getVerdi();
        final String gruppe = PubliserStønadstatistikkHendelseTask.TASKTYPE + "-" + saksnummer;
        pd.setGruppe(gruppe);
        pd.setSekvens(lagSekvensnummer(behandling));
        pd.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(), behandling.getAktørId().getId());
        pd.setProperty(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY, saksnummer);
        pd.setCallIdFraEksisterende();
        
        return pd;
    }
    
    private static String lagSekvensnummer(Behandling behandling) {
        // Sekvensnummeret bør la seg sortere slik at meldingene blir sendt (når det er mulig) i riktig rekkefølge.

        int antallSiffer = 5; // et vilkårlig antall siffer som er stort nok til å holde på et versjonsnummer
        String sekvensnummerFagsak = tallMedPrefiks(behandling.getFagsak().getVersjon(), antallSiffer);
        String sekvensnummerBehandling = tallMedPrefiks(behandling.getVersjon(), antallSiffer);
        return String.format("%s-%s", sekvensnummerFagsak, sekvensnummerBehandling);
    }
    
    private static String tallMedPrefiks(long versjon, int antallSiffer) {
        if (versjon >= Math.pow(10, antallSiffer)) {
            throw new IllegalArgumentException("Versjonsnummeret er for stort");
        }
        return StringUtils.leftPad(Long.toString(versjon), antallSiffer, '0');
    }
}
