package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

public interface InfotrygdKafkaProducerFeil extends DeklarerteFeil {

    InfotrygdKafkaProducerFeil FACTORY = FeilFactory.create(InfotrygdKafkaProducerFeil.class);

    // todo: fyll ut riktige koder...
    @ManglerTilgangFeil(feilkode = "K9-FEED-821005", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
    Feil feilIPålogging(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "K9-FEED-925469", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil uventetFeil(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "K9-FEED-127608", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
    Feil retriableExceptionMotKaka(String topic, RetriableException e);

    @IntegrasjonFeil(feilkode = "K9-FEED-811208", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil annenExceptionMotKafka(String topic, KafkaException e);


}
