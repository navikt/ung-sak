package no.nav.k9.sak.historikk.kafka;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.familie.topic.Topic;
import no.nav.familie.topic.TopicManifest;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@Dependent
public class HistorikkStreamKafkaProperties {

    private final String bootstrapServers;
    private final Topic kontraktTopic;
    private final String username;
    private final String password;
    private final String topic;
    private String applicationId;

    @Inject
    HistorikkStreamKafkaProperties(@KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                   @KonfigVerdi("systembruker.username") String username,
                                   @KonfigVerdi("systembruker.password") String password,
                                   @KonfigVerdi(value = "kafka.historikkinnslag.topic", defaultVerdi = "privat-k9-historikkinnslag") String topic) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.username = username;
        this.password = password;
        this.applicationId = "ID-" + topic;
        this.kontraktTopic = TopicManifest.HISTORIKK_HENDELSE;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    String getTopic() {
        return topic;
    }

    boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    String getClientId() {
        return "KC-" + topic;
    }

    @SuppressWarnings("resource")
    Class<?> getKeyClass() {
        return kontraktTopic.getSerdeKey().getClass();
    }

    @SuppressWarnings("resource")
    Class<?> getValueClass() {
        return kontraktTopic.getSerdeValue().getClass();
    }

    String getApplicationId() {
        return applicationId;
    }
}
