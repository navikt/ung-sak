package no.nav.k9.sak.domene.vedtak.observer;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

public interface HendelseKafkaProducerFeil extends DeklarerteFeil {

    HendelseKafkaProducerFeil FACTORY = FeilFactory.create(HendelseKafkaProducerFeil.class);

    @ManglerTilgangFeil(feilkode = "FP-HENDELSE-821005", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
    Feil feilIPålogging(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-925469", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil uventetFeil(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-127608", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
    Feil retriableExceptionMotKaka(String topic, RetriableException e);

    @IntegrasjonFeil(feilkode = "FP-HENDELSE-811208", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil annenExceptionMotKafka(String topic, KafkaException e);
}
