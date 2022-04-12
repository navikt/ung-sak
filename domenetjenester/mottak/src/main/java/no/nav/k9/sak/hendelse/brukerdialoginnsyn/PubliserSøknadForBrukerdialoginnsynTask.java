package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import java.time.ZonedDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.PsbSøknadsinnhold;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
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
        if (mottattDokument.getStatus() == DokumentStatus.UGYLDIG) {
            logger.info("Ignorerer ugyldig dokument: " + mottattDokumentId);
            return;
        }

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

        final ProsessTaskData pd =  ProsessTaskData.forProsessTask(PubliserSøknadForBrukerdialoginnsynTask.class);
        final Fagsak fagsak = behandling.getFagsak();
        final String saksnummer = fagsak.getSaksnummer().getVerdi();
        final String gruppe = PubliserSøknadForBrukerdialoginnsynTask.TASKTYPE + "-" + saksnummer;
        pd.setGruppe(gruppe);
        pd.setSekvens(PubliserJsonForBrukerdialoginnsynTask.lagSekvensnummer(behandling));
        pd.setSaksnummer(saksnummer);
        pd.setAktørId(fagsak.getAktørId().getId());
        pd.setProperty(PLEIETRENGENDE_AKTØR_ID, fagsak.getPleietrengendeAktørId().getId());
        pd.setProperty(MOTTATT_DOKUMENT_ID, mottattDokument.getId().toString());
        pd.setCallIdFraEksisterende();

        return pd;
    }
}
