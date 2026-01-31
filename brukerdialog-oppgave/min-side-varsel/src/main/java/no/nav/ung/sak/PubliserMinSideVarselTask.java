package no.nav.ung.sak;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.kafka.GenerellKafkaProducer;
import no.nav.k9.felles.integrasjon.kafka.KafkaPropertiesBuilder;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.tms.varsel.action.EksternKanal;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@ApplicationScoped
@ProsessTask(PubliserMinSideVarselTask.TASKTYPE)
public class PubliserMinSideVarselTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "minside.publiservarsel";
    public static final String OPPGAVE_REFERANSE = "oppgaveReferanse";
    public static final String VARSEL_TEKST = "varsel_tekst";
    public static final String VARSEL_LENKE = "varsel_lenke";

    private static final Logger log = LoggerFactory.getLogger(PubliserMinSideVarselTask.class);

    private GenerellKafkaProducer producer;
    private Pdl pdl;
    private String appNamespace;
    private String appNavn;
    private String clusterNavn;

    PubliserMinSideVarselTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserMinSideVarselTask(
        Pdl pdl,
        @KonfigVerdi(value = "kafka.minside.varsel.topic", defaultVerdi = "min-side.aapen-brukervarsel-v1") String topic,
        @KonfigVerdi(value = "KAFKA_BROKERS") String kafkaBrokers,
        @KonfigVerdi(value = "KAFKA_TRUSTSTORE_PATH", required = false) String trustStorePath,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String trustStorePassword,
        @KonfigVerdi(value = "KAFKA_KEYSTORE_PATH", required = false) String keyStoreLocation,
        @KonfigVerdi(value = "KAFKA_CREDSTORE_PASSWORD", required = false) String keyStorePassword,
        @KonfigVerdi(value = "NAIS_NAMESPACE", defaultVerdi = "k9saksbehandling") String appNamespace,
        @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "ung-sak") String appNavn,
        @KonfigVerdi(value = "NAIS_CLUSTER_NAME", defaultVerdi = "prod-gcp") String clusterNavn
    ) {
        this.pdl = pdl;
        this.appNamespace = appNamespace;
        this.appNavn = appNavn;
        this.clusterNavn = clusterNavn;
        Properties aivenProps = new KafkaPropertiesBuilder()
            .clientId("KP-" + topic)
            .bootstrapServers(kafkaBrokers)
            .truststorePath(trustStorePath)
            .truststorePassword(trustStorePassword)
            .keystorePath(keyStoreLocation)
            .keystorePassword(keyStorePassword)
            .buildForProducerAiven();

        producer = new GenerellKafkaProducer(topic, aivenProps);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String oppgaveReferanse = prosessTaskData.getPropertyValue(OPPGAVE_REFERANSE);
        String aktørId = prosessTaskData.getAktørId();
        String personIdent = pdl.hentPersonIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Finner ikke personident for aktørId"));
        String kafkaValueJson = OpprettVarselBuilder.newInstance()
            .withType(Varseltype.Oppgave)
            .withVarselId(oppgaveReferanse)
            .withSensitivitet(Sensitivitet.High)
            .withIdent(personIdent)
            .withTekst("nb", prosessTaskData.getPropertyValue(VARSEL_TEKST))
            .withLink(prosessTaskData.getPropertyValue(VARSEL_LENKE))
            .withEksternVarsling(
                OpprettVarselBuilder.eksternVarsling()
                    .withPreferertKanal(EksternKanal.SMS)
            )
            .withProdusent(clusterNavn, appNamespace, appNavn)
            .build();
        RecordMetadata recordMetadata = producer.sendJsonMedNøkkel(oppgaveReferanse, kafkaValueJson);
        log.info("Sendte melding til  {} partition {} offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of(OPPGAVE_REFERANSE, ProsessTaskData.AKTØR_ID, VARSEL_TEKST, VARSEL_LENKE);
    }
}
