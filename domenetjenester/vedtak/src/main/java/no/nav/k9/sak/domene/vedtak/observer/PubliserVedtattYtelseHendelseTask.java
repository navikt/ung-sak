package no.nav.k9.sak.domene.vedtak.observer;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfig;
import no.nav.k9.felles.integrasjon.kafka.GenerellKafkaProducer;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerAndreSakerTask;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;

@ApplicationScoped
@ProsessTask(PubliserVedtattYtelseHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class PubliserVedtattYtelseHendelseTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "vedtak.publiserHendelse";
    private static final Logger log = LoggerFactory.getLogger(VurderOmVedtakPåvirkerAndreSakerTask.class);

    private BehandlingRepository behandlingRepository;
    private VedtattYtelseTjeneste vedtakTjeneste;
    private GenerellKafkaProducer producer;
    private Validator validator;

    private Instance<InformasjonselementerUtleder> informasjonselementer;
    private ProsessTaskTjeneste taskTjeneste;

    PubliserVedtattYtelseHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtattYtelseHendelseTask(
        BehandlingRepositoryProvider repositoryProvider,
        VedtattYtelseTjeneste vedtakTjeneste,
        ProsessTaskTjeneste taskTjeneste,
        @Any Instance<InformasjonselementerUtleder> informasjonselementer,
        @KonfigVerdi("kafka.fattevedtak.topic") String topic,
        @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServersAiven,
        @KonfigVerdi("bootstrap.servers") String bootstrapServersOnPrem,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword
    ) {
        this.taskTjeneste = taskTjeneste;
        this.informasjonselementer = informasjonselementer;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakTjeneste = vedtakTjeneste;

        boolean kjørerIMiljø = Environment.current().isProd() || Environment.current().isDev();
        if (kjørerIMiljø) {
            var aivenPropsBuilder = new KafkaPropertiesBuilder()
                .clientId("KP-" + topic).bootstrapServers(bootstrapServersAiven);

            Properties aivenProps = aivenPropsBuilder
                .truststorePath(trustStorePath)
                .truststorePassword(trustStorePassword)
                .keystorePath(keyStoreLocation)
                .keystorePassword(keyStorePassword)
                .buildForProducerAiven();

            producer = new GenerellKafkaProducer(topic, aivenProps);
        } else {
            //konfigurasjon for bruk mot VTP
            var onPremPropsBuilder = new KafkaPropertiesBuilder()
                .clientId("KP-" + topic).bootstrapServers(bootstrapServersOnPrem);

            Properties onPremProps = onPremPropsBuilder
                .username("vtp")
                .password("vtp")
                .buildForProducerJaas();
            producer = new GenerellKafkaProducer(topic, onPremProps);
        }


        @SuppressWarnings("resource")
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String behandingIdString = prosessTaskData.getBehandlingId();
        if (behandingIdString != null && !behandingIdString.isEmpty()) {
            long behandlingId = Long.parseLong(behandingIdString);

            Optional<Behandling> behandlingOptional = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);
            if (behandlingOptional.isPresent()) {
                var behandling = behandlingOptional.get();
                logContext(behandling);

                if (!erFagsakYtelseBasert(behandling)) {
                    // ingenting å publisere her
                    return;
                }

                String payload = generatePayload(behandling);

                var fagsakYtelseType = behandling.getFagsakYtelseType();
                log.info("Mottatt ytelse-vedtatt hendelse med ytelse='{}' saksnummer='{}', sjekker behovet for revurdering", fagsakYtelseType, behandling.getFagsak().getSaksnummer());
                var vurderOmVedtakPåvirkerSakerTjeneste = VurderOmVedtakPåvirkerSakerTjeneste.finnTjenesteHvisStøttet(fagsakYtelseType);

                if (vurderOmVedtakPåvirkerSakerTjeneste.isPresent()) {
                    ProsessTaskData taskData = ProsessTaskData.forProsessTask(VurderOmVedtakPåvirkerAndreSakerTask.class);
                    taskData.setPayload(payload);
                    taskTjeneste.lagre(taskData);
                }
                String key = behandling.getFagsak().getSaksnummer().getVerdi();
                RecordMetadata recordMetadata = producer.sendJsonMedNøkkel(key, payload);
                log.info("Sendte melding til  {} partition {} offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
            }
        }
    }

    private boolean erFagsakYtelseBasert(Behandling behandling) {
        var infoelementer = finnTjeneste(behandling.getFagsakYtelseType(), behandling.getType());
        return !(infoelementer.utled(behandling.getType()).isEmpty());
    }

    private String generatePayload(Behandling behandling) {
        Ytelse ytelse = vedtakTjeneste.genererYtelse(behandling);

        Set<ConstraintViolation<Ytelse>> violations = validator.validate(ytelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            List<String> allErrors = violations
                .stream()
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage()).toList();
            throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil \n " + allErrors);
        }
        return JacksonJsonConfig.toJson(ytelse, PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere);
    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType);
    }

}
