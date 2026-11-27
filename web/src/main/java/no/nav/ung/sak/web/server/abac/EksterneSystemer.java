package no.nav.ung.sak.web.server.abac;

public final class EksterneSystemer {

    public static final String TILLEGGSSTØNADER_DEV = "dev-gcp:tilleggsstonader:tilleggsstonader-integrasjoner";
    public static final String TILLEGGSSTØNADER_PROD = "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner";

    // Vedtatt ytelse (TSFF-3395). Kun dev inntil hjemmel er avklart.
    public static final String AAP_DEV = "dev-gcp:aap:behandlingsflyt";
    public static final String DAGPENGER_DEV = "dev-gcp:teamdagpenger:dp-oppslag-ytelser";
    public static final String TILTAKSPENGER_DEV = "dev-gcp:tpts:tiltakspenger-saksbehandling-api";
    public static final String BIDRAG_DEV = "dev-gcp:bidrag:bidrag-grunnlag";
    public static final String BIDRAG_FEATURE_DEV = "dev-gcp:bidrag:bidrag-grunnlag-feature";

    /** For manuell testing i dev med tokens fra azure-token-generator. Skal aldri finnes i prod. */
    public static final String AZURE_TOKEN_GENERATOR_DEV = "dev-gcp:nais:azure-token-generator";

    private EksterneSystemer() {
    }
}
