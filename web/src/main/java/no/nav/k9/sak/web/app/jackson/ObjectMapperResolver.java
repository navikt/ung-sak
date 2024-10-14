package no.nav.k9.sak.web.app.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;

@Provider
@Produces({ MediaType.APPLICATION_JSON })
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
    @Context
    private HttpHeaders headers;

    private final ObjectMapper baseObjektMapper;
    private final ObjectMapper overstyrKodeverdiAlltidSomStringMapper;
    private final ObjectMapper overstyrKalkulusKodeverdiSomStringMapper;
    private final ObjectMapper defaultObjektMapper;
    private final ObjectMapper openapiObjektMapper;


    /**
     * Oppretter ulike varianter av ObjectMapper. Klient kan deretter velge hvilken som skal brukast ved å sette header i request.
     */
    public ObjectMapperResolver(final boolean featureFlagKodeverkAktiverKalkulusString) {
        this.baseObjektMapper = ObjectMapperFactory.createBaseObjectMapper();
        this.overstyrKodeverdiAlltidSomStringMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.KODE_STRING, false));
        this.overstyrKalkulusKodeverdiSomStringMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.INGEN, false));
        // defaultObjektMapper brukast når input header for overstyring ikkje er satt.
        // Bruker samme logikk som har vore pr no. Det vil seie overstyring av Kalkulus Kodeverdi serialisering til objekt, så lenge ikkje feature flagg for string serialisering er aktivt.
        // Når alle klienter kan handtere at Kalkulus Kodeverdi kjem som string kan denne sannsynlegvis settast lik baseObjektMapper.
        this.defaultObjektMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.INGEN, !featureFlagKodeverkAktiverKalkulusString));
        // openaapiObjektMapper bør brukast viss ein ønsker at enums skal bli serialisert slik openapi spesifikasjon tilseier.
        this.openapiObjektMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOpenapiCompatSerializerModule(this.baseObjektMapper));
    }

    public ObjectMapperResolver() {
        this(getFeatureFlagKodeverkAktiverKalkulusString());
    }

    private String getJsonSerializerOptionHeaderValue() {
        // Denne verdi er også hardkoda i k9-sak-web jsonSerializerOption.ts
        final var headerValues = headers.getRequestHeader("X-Json-Serializer-Option");
        if(headerValues != null) {
            final var firstValue = headerValues.getFirst();
            if(firstValue != null) {
                return firstValue;
            }
        }
        return "";
    }

    public static boolean getFeatureFlagKodeverkAktiverKalkulusString() {
        return Environment.current().getProperty("KODEVERK_AKTIVER_KALKULUS_STRING", Boolean.class, false);
    }

    /**
     * Resolver kva ObjectMapper som skal brukast for gitt type.
     * Denne er i tillegg dynamisk basert på header frå innkommande request. For at dette skal fungere må ein bruke
     * CustomJacksonJsonProvider som skrur av caching av resolved ObjectMapper.
     */
    @Override
    public ObjectMapper getContext(Class<?> type) {
        // TODO Dette bør gjøres bedre slik at registrering av ObjectMapper gjøres lokalt i Rest-tjenesten.
        if (type.isAssignableFrom(Søknad.class)) {
            return JsonUtils.getObjectMapper();
        }
        final String serializerOption = this.getJsonSerializerOptionHeaderValue();
        return switch (serializerOption) {
            // Kompatibilitet for verdikjede test klient, istadenfor feature flag på server:
            case "kodeverdi-kalkulus-string" -> this.overstyrKalkulusKodeverdiSomStringMapper;
            case "kodeverdi-string" -> this.overstyrKodeverdiAlltidSomStringMapper;
            case "base" -> this.baseObjektMapper;
            case "openapi-compat" -> this.openapiObjektMapper; // <- Også hardkoda i k9-sak-web jsonSerializerOption.ts
            // Viss ingen gyldig header verdi, gjer det samme som før basert på feature flag.
            default -> this.defaultObjektMapper;
        };
    }

}
