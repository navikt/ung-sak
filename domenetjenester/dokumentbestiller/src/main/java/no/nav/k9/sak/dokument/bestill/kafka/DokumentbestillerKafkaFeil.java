package no.nav.k9.sak.dokument.bestill.kafka;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.RetriableException;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;

public interface DokumentbestillerKafkaFeil extends DeklarerteFeil {

    DokumentbestillerKafkaFeil FACTORY = FeilFactory.create(DokumentbestillerKafkaFeil.class);

    @ManglerTilgangFeil(feilkode = "K9-DOK-821005", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
    Feil feilIPålogging(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "K9-DOK-925469", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil uventetFeil(String topic, Exception e);

    @IntegrasjonFeil(feilkode = "K9-DOK-127608", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
    Feil retriableExceptionMotKaka(String topic, RetriableException e);

    @IntegrasjonFeil(feilkode = "K9-DOK-811208", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
    Feil annenExceptionMotKafka(String topic, KafkaException e);

}
