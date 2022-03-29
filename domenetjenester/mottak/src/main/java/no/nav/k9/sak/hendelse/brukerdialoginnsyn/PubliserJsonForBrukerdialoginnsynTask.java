package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
@ProsessTask(PubliserJsonForBrukerdialoginnsynTask.TASKTYPE)
public class PubliserJsonForBrukerdialoginnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "brukerdialoginnsyn.publiserJson";
    private static final Logger logger = LoggerFactory.getLogger(PubliserJsonForBrukerdialoginnsynTask.class);

    private BrukerdialoginnsynMeldingProducer meldingProducer;


    public PubliserJsonForBrukerdialoginnsynTask() {}

    @Inject
    public PubliserJsonForBrukerdialoginnsynTask(MottatteDokumentRepository mottatteDokumentRepository, BrukerdialoginnsynMeldingProducer meldingProducer) {
        this.meldingProducer = meldingProducer;
    }


    @Override
    public void doTask(ProsessTaskData pd) {
        final String key = Objects.requireNonNull(pd.getSaksnummer());
        final String json = pd.getPayloadAsString();

        logger.info("Publiserer JSON-hendelse til brukerdialoginnsyn. Key: '{}'", key);

        meldingProducer.send(key, json);
    }


    public static ProsessTaskData createProsessTaskData(Behandling behandling, String json) {
        Objects.requireNonNull(behandling);
        Objects.requireNonNull(json);

        final ProsessTaskData pd =  ProsessTaskData.forProsessTask(PubliserJsonForBrukerdialoginnsynTask.class);
        final Fagsak fagsak = behandling.getFagsak();
        final String saksnummer = fagsak.getSaksnummer().getVerdi();
        final String gruppe = PubliserJsonForBrukerdialoginnsynTask.TASKTYPE + "-" + saksnummer;
        pd.setGruppe(gruppe);
        pd.setSekvens(lagSekvensnummer(behandling));
        pd.setSaksnummer(saksnummer);
        pd.setAktørId(fagsak.getAktørId().getId());
        pd.setPayload(json);
        pd.setCallIdFraEksisterende();

        return pd;
    }

    static String lagSekvensnummer(Behandling behandling) {
        // Sekvensnummeret bør la seg sortere slik at meldingene blir sendt (når det er mulig) i riktig rekkefølge.
        int antallSiffer = 10; // et vilkårlig antall siffer som er stort nok til å holde på et versjonsnummer
        String sekvensnummerFagsak = tallMedPrefiks(behandling.getFagsak().getVersjon(), antallSiffer);
        String sekvensnummerBehandling = tallMedPrefiks(behandling.getVersjon(), antallSiffer);
        return String.format("%s-%s", sekvensnummerFagsak, sekvensnummerBehandling);
    }

    private static String tallMedPrefiks(long versjon, int antallSiffer) {
        if (Long.toString(versjon).length() > antallSiffer) {
            throw new IllegalArgumentException("Versjonsnummeret er for stort");
        }
        return StringUtils.leftPad(Long.toString(versjon), antallSiffer, '0');
    }
}
