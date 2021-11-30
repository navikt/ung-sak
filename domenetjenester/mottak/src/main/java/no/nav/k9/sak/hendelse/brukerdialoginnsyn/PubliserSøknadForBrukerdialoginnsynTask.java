package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import java.time.ZonedDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.PsbSøknadsinnhold;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;

@ApplicationScoped
@ProsessTask(PubliserSøknadForBrukerdialoginnsynTask.TASKTYPE)
public class PubliserSøknadForBrukerdialoginnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "brukerdialoginnsyn.publiserSoknad";
    private static final Logger logger = LoggerFactory.getLogger(PubliserSøknadForBrukerdialoginnsynTask.class);
    private static final String PLEIETRENGENDE_AKTØR_ID = "pleietrengendeAktoerId";
    private static final String MOTTATT_DOKUMENT_ID = "mottattDokumentId";
    
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BrukerdialoginnsynMeldingProducer meldingProducer;

    
    public PubliserSøknadForBrukerdialoginnsynTask() {}

    @Inject
    public PubliserSøknadForBrukerdialoginnsynTask(MottatteDokumentRepository mottatteDokumentRepository,
            BrukerdialoginnsynMeldingProducer meldingProducer) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.meldingProducer = meldingProducer;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        final String key = Objects.requireNonNull(pd.getSaksnummer());
        final String aktørId = Objects.requireNonNull(pd.getAktørId());
        final String pleietrengendeAktørId = Objects.requireNonNull(pd.getPropertyValue(PLEIETRENGENDE_AKTØR_ID));
        final long mottattDokumentId = Long.parseLong(pd.getPropertyValue(MOTTATT_DOKUMENT_ID));
        
        final MottattDokument mottattDokument = mottatteDokumentRepository.hentMottattDokument(mottattDokumentId).orElseThrow();
        
        /*
         * Det er fint at vi deserialiserer og reserialiserer søknad. Dette sikrer at vi kun sender
         * søknader som følger formatet.
         */
        final Søknad søknad = JsonUtils.fromString(mottattDokument.getPayload(), Søknad.class);
        final PsbSøknadsinnhold søknadsinnhold = new PsbSøknadsinnhold(mottattDokument.getJournalpostId().getVerdi(), aktørId, pleietrengendeAktørId, søknad);
        final InnsynHendelse<PsbSøknadsinnhold> hendelse = new InnsynHendelse<>(ZonedDateTime.now(), søknadsinnhold);
        final String json = JsonUtils.toString(hendelse);
                
        logger.info("Publiserer hendelse til brukerdialoginnsyn. Key: '{}'", key);

        meldingProducer.send(key, json);
    }
    
    
    public static ProsessTaskData createProsessTaskData(Behandling behandling, MottattDokument mottattDokument) {
        Objects.requireNonNull(behandling);
        Objects.requireNonNull(mottattDokument.getId());
        
        final ProsessTaskData pd = new ProsessTaskData(PubliserSøknadForBrukerdialoginnsynTask.TASKTYPE);
        final Fagsak fagsak = behandling.getFagsak();
        final String saksnummer = fagsak.getSaksnummer().getVerdi();
        final String gruppe = PubliserSøknadForBrukerdialoginnsynTask.TASKTYPE + "-" + saksnummer;
        pd.setGruppe(gruppe);
        pd.setSekvens(lagSekvensnummer(behandling));
        pd.setSaksnummer(saksnummer);
        pd.setAktørId(fagsak.getAktørId().getId());
        pd.setProperty(PLEIETRENGENDE_AKTØR_ID, fagsak.getPleietrengendeAktørId().getId());
        pd.setProperty(MOTTATT_DOKUMENT_ID, mottattDokument.getId().toString());
        pd.setCallIdFraEksisterende();
        
        return pd;
    }
    
    private static String lagSekvensnummer(Behandling behandling) {
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
