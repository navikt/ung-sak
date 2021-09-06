package no.nav.k9.sak.dokument.bestill.kafka;

import static no.nav.k9.sak.dokument.bestill.kafka.DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER_SEPARATOR;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import no.nav.k9.formidling.kontrakt.dokumentdataparametre.DokumentdataParametreK9;
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling;
import no.nav.k9.formidling.kontrakt.kodeverk.AvsenderApplikasjon;
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.formidling.kontrakt.kodeverk.Mottaker;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

@ApplicationScoped
@ProsessTask(DokumentbestillerKafkaTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DokumentBestillerKafkaTask implements ProsessTaskHandler {

    private DokumentbestillingProducer dokumentbestillingProducer;
    private BehandlingRepository behandlingRepository;
    private Validator validator;

    DokumentBestillerKafkaTask() {
        // for CDI proxy
    }

    @Inject
    public DokumentBestillerKafkaTask(DokumentbestillingProducer dokumentbestillingProducer,
                                      BehandlingRepository behandlingRepository) {
        this.dokumentbestillingProducer = dokumentbestillingProducer;
        this.behandlingRepository = behandlingRepository;

        @SuppressWarnings("resource")
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    private static FagsakYtelseType mapYtelse(no.nav.k9.kodeverk.behandling.FagsakYtelseType fpsakYtelseKode) {
        if (fpsakYtelseKode == no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMP) {
            return FagsakYtelseType.OMSORGSPENGER;
        }
        return FagsakYtelseType.fraKode(fpsakYtelseKode.getKode());
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String behandlingIdStr = prosessTaskData.getBehandlingId() != null ? prosessTaskData.getBehandlingId() : prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID);
        Long behandlingId = Long.valueOf(behandlingIdStr);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingProsessTask.logContext(behandling);

        Dokumentbestilling dokumentbestilling = mapDokumentbestilling(behandling, prosessTaskData);
        String json = serialiser(dokumentbestilling);
        dokumentbestillingProducer.publiserDokumentbestillingJson(json);
    }

    private Dokumentbestilling mapDokumentbestilling(Behandling behandling, ProsessTaskData prosessTaskData) {

        var dto = new Dokumentbestilling();
        dto.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        dto.setAktørId(behandling.getAktørId().getId());
        dto.setEksternReferanse(behandling.getUuid().toString());
        dto.setDokumentbestillingId(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID));
        dto.setDokumentMal(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE));
        dto.setOverstyrtMottaker(mapMottaker(prosessTaskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.OVERSTYRT_MOTTAKER)));
        dto.setDokumentdata(fraJson(prosessTaskData));
        dto.setYtelseType(mapYtelse(behandling.getFagsakYtelseType()));
        dto.setAvsenderApplikasjon(AvsenderApplikasjon.K9SAK);
        valider(dto);

        return dto;
    }

    private DokumentdataParametreK9 fraJson(ProsessTaskData prosessTaskData) {
        String payloadAsString = prosessTaskData.getPayloadAsString();

        if (payloadAsString == null) return new DokumentdataParametreK9();

        if (JsonObjectMapper.readTree(payloadAsString).isObject()) {
            return JsonObjectMapper.fromJson(payloadAsString, DokumentdataParametreK9.class);
        }

        var dokumentdataParametreK9 = new DokumentdataParametreK9();
        dokumentdataParametreK9.setFritekst(JsonObjectMapper.fromJson(payloadAsString, String.class));
        return dokumentdataParametreK9;
    }

    private Mottaker mapMottaker(String prosessDataProperty) {
        if (prosessDataProperty == null) {
            return null;
        }

        var mottaker = prosessDataProperty.split(OVERSTYRT_MOTTAKER_SEPARATOR);
        return new Mottaker(mottaker[0], IdType.valueOf(mottaker[1]));
    }

    private void valider(Dokumentbestilling dokumentbestillingDto) {
        Set<ConstraintViolation<Dokumentbestilling>> violations = validator.validate(dokumentbestillingDto);
        if (!violations.isEmpty()) {
            // Har feilet validering
            List<String> allErrors = violations
                .stream()
                .map(it -> it.getPropertyPath().toString() + " :: " + it.getMessage())
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Dokumentbestilling valideringsfeil \n " + allErrors);
        }
    }

    private String serialiser(Dokumentbestilling dto) {
        try {
            return JsonObjectMapper.getJson(dto);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Klarte ikke å serialisere dokumentbestilling for behandling=" + dto.getEksternReferanse() + ", mal=" + dto.getDokumentMal(), e); // NOSONAR
        }
    }
}
