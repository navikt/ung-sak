package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;
/**
 * @deprecated single impl interface
 *
 */
@Deprecated
public interface MeldingProducer {

    void sendJson(String json);

    void sendJsonMedNøkkel(String nøkkel, String json);

    void flushAndClose();

}
