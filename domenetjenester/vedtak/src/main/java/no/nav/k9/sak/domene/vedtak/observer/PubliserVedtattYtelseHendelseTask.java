package no.nav.k9.sak.domene.vedtak.observer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.JacksonJsonConfig;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@ProsessTask(PubliserVedtattYtelseHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserVedtattYtelseHendelseTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "vedtak.publiserHendelse";

    private BehandlingRepository behandlingRepository;
    private VedtattYtelseTjeneste vedtakTjeneste;
    private HendelseProducer producer;
    private Validator validator;

    private Instance<InformasjonselementerUtleder> informasjonselementer;

    PubliserVedtattYtelseHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserVedtattYtelseHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                             VedtattYtelseTjeneste vedtakTjeneste,
                                             @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                             @KonfigVerdi("kafka.fattevedtak.topic") String topic,
                                             @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                             @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                             @KonfigVerdi("systembruker.username") String username,
                                             @KonfigVerdi("systembruker.password") String password) {
        this.informasjonselementer = informasjonselementer;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakTjeneste = vedtakTjeneste;
        this.producer = new HendelseProducer(topic, bootstrapServers, schemaRegistryUrl, username, password);

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
                BehandlingProsessTask.logContext(behandling);

                if (!erFagsakYtelseBasert(behandling)) {
                    // ingenting å publisere her
                    return;
                }

                String payload = generatePayload(behandling);
                producer.sendJson(payload);
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
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage())
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil \n " + allErrors);
        }
        return JacksonJsonConfig.toJson(ytelse, PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere);
    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(InformasjonselementerUtleder.class, informasjonselementer, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke utleder for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType));
    }

}
