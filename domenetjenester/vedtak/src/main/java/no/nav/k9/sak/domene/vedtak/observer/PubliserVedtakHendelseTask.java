package no.nav.k9.sak.domene.vedtak.observer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfig;
import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfigKodeverdiSomString;
import no.nav.k9.felles.integrasjon.kafka.GenerellKafkaProducer;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;

@ApplicationScoped
@ProsessTask(PubliserVedtakHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class PubliserVedtakHendelseTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "vedtak.publiserVedtakhendelse";

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private boolean kodeverkSomStringTopics;
    private GenerellKafkaProducer producer;
    private Validator validator;

    PubliserVedtakHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtakHendelseTask(
        BehandlingRepositoryProvider repositoryProvider,
        BehandlingVedtakRepository behandlingVedtakRepository,
        BehandlingLåsRepository behandlingLåsRepository,
        @KonfigVerdi("kafka.vedtakhendelse.topic") String topic,
        @KonfigVerdi("kafka.vedtakhendelse.aiven.topic") String topicV2,
        @KonfigVerdi(value = "KAFKA_BROKERS") String bootstrapServersAiven,
        @KonfigVerdi("bootstrap.servers") String bootstrapServersOnPrem,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword,
        @KonfigVerdi("systembruker.username") String username,
        @KonfigVerdi("systembruker.password") String password,
        @KonfigVerdi(value = "KODEVERK_SOM_STRING_TOPICS", defaultVerdi = "false") boolean kodeverkSomStringTopics

    ) {
        super(behandlingLåsRepository);

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.kodeverkSomStringTopics = kodeverkSomStringTopics;

        boolean aivenEnabled = !Environment.current().isLocal(); //har ikke støtte i vtp

        String _topicName = aivenEnabled ? topicV2 : topic;
        String _bootstrapServer = aivenEnabled ? bootstrapServersAiven : bootstrapServersOnPrem;

        var builder = new KafkaPropertiesBuilder()
            .clientId("KP-" + _topicName).bootstrapServers(_bootstrapServer);

        var props = aivenEnabled ?
            builder
                .truststorePath(trustStorePath)
                .truststorePassword(trustStorePassword)
                .keystorePath(keyStoreLocation)
                .keystorePassword(keyStorePassword)
                .buildForProducerAiven() :
            builder
                .username(username)
                .password(password)
                .buildForProducerJaas();


        producer = new GenerellKafkaProducer(_topicName, props);

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

            behandlingOptional.ifPresent((behandling) -> {
                logContext(behandling);
                BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
                if (!vedtak.getErPublisert()) {
                    BehandlingProsessTask.logContext(behandling);
                    String payload = generatePayload(behandling, vedtak);
                    producer.sendJson(payload);
                    vedtak.setErPublisert();
                }
            });
        }
    }

    private String generatePayload(Behandling behandling, BehandlingVedtak vedtak) {
        var vedtakHendelse = genererVedtakHendelse(behandling, vedtak);

        Set<ConstraintViolation<VedtakHendelse>> violations = validator.validate(vedtakHendelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            List<String> allErrors = violations
                .stream()
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage())
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Vedtakhendelse valideringsfeil \n " + allErrors);
        }

        if (kodeverkSomStringTopics) {
            return JacksonJsonConfigKodeverdiSomString.toJson(vedtakHendelse, PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere);
        } else {
            return JacksonJsonConfig.toJson(vedtakHendelse, PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere);
        }
    }

    private VedtakHendelse genererVedtakHendelse(Behandling behandling, BehandlingVedtak vedtak) {
        var fagsak = behandling.getFagsak();

        var vedtakHendelse = new VedtakHendelse();
        vedtakHendelse.setBehandlingId(behandling.getUuid());
        vedtakHendelse.setBehandlingType(behandling.getType());
        vedtakHendelse.setFagsakYtelseType(behandling.getFagsakYtelseType());
        vedtakHendelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        vedtakHendelse.setStatus(behandling.getFagsak().getStatus());
        vedtakHendelse.setFagsystem(no.nav.k9.kodeverk.Fagsystem.K9SAK);
        vedtakHendelse.setAktør(behandling.getAktørId());
        vedtakHendelse.setBehandlingResultatType(behandling.getBehandlingResultatType());
        vedtakHendelse.setVedtakResultat(vedtak.getVedtakResultatType());
        vedtakHendelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        vedtakHendelse.setFagsakPeriode(fagsak.getPeriode().tilPeriode());
        vedtakHendelse.setPleietrengendeAktørId(fagsak.getPleietrengendeAktørId());
        vedtakHendelse.setRelatertPartAktørId(fagsak.getRelatertPersonAktørId());
        return vedtakHendelse;
    }
}
