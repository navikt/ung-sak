package no.nav.k9.sak.domene.vedtak.observer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfig;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@ProsessTask(PubliserVedtakHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserVedtakHendelseTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "vedtak.publiserVedtakhendelse";
    public static final String KEY = "vedtattBehandlingId";

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private HendelseProducer producer;
    private Validator validator;

    PubliserVedtakHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtakHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                      BehandlingVedtakRepository behandlingVedtakRepository,
                                      @KonfigVerdi("kafka.vedtakhendelse.topic") String topic,
                                      @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                      @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                      @KonfigVerdi("systembruker.username") String username,
                                      @KonfigVerdi("systembruker.password") String password) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.producer = new HendelseProducer(topic, bootstrapServers, schemaRegistryUrl, username, password);

        @SuppressWarnings("resource")
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String behandingIdString = prosessTaskData.getPropertyValue(KEY);
        if (behandingIdString != null && !behandingIdString.isEmpty()) {
            long behandlingId = Long.parseLong(behandingIdString);

            Optional<Behandling> behandlingOptional = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);
            behandlingOptional.ifPresent((behandling) -> {
                BehandlingProsessTask.logContext(behandling);
                String payload = generatePayload(behandling);
                producer.sendJson(payload);
                }
            );
        }
    }

    private String generatePayload(Behandling behandling) {
        var vedtakHendelse= genererVedtakHendelse(behandling);

        Set<ConstraintViolation<VedtakHendelse>> violations = validator.validate(vedtakHendelse);
        if (!violations.isEmpty()) {
            // Har feilet validering
            List<String> allErrors = violations
                .stream()
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage())
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Vedtakhendelse valideringsfeil \n " + allErrors);
        }
        return JacksonJsonConfig.toJson(vedtakHendelse, PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere);
    }


    private VedtakHendelse genererVedtakHendelse(Behandling behandling) {
        final BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();

        final VedtakHendelse vedtakHendelse = new VedtakHendelse();
            vedtakHendelse.setBehandlingId(behandling.getUuid());
            vedtakHendelse.setType(behandling.getFagsakYtelseType());
            vedtakHendelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            vedtakHendelse.setStatus(behandling.getFagsak().getStatus());
            vedtakHendelse.setFagsystem(no.nav.k9.kodeverk.Fagsystem.K9SAK);
            vedtakHendelse.setAktør(behandling.getAktørId());
            vedtakHendelse.setVedtakResultat(vedtak.getVedtakResultatType());
            vedtakHendelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());

            return vedtakHendelse;
    }
}
