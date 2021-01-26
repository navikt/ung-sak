package no.nav.k9.sak.dokument.bestill.kafka;

public class DokumentbestillerKafkaTaskProperties {

    public static final String TASKTYPE = "dokumentbestiller.kafka.bestilldokument";

    public static final String BEHANDLING_ID = "behandlingId";
    public static final String DOKUMENT_MAL_TYPE = "dokumentMalType";
    public static final String OVERSTYRT_MOTTAKER = "overstyrtMottaker";
    public static final String OVERSTYRT_MOTTAKER_SEPARATOR = ";";

    public static final String BESTILLING_UUID = "bestillingUuid";


    private DokumentbestillerKafkaTaskProperties() {
        // Skal ikke konstrueres
    }
}
