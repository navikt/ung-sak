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
    private final ObjectMapper kodeverdiAlltidSomObjektMapper;
    private final ObjectMapper kodeverdiAlltidSomStringMapper;
    private final ObjectMapper sakKodeverdiStandardKalkulusKodeverdiStringMapper;
    private final ObjectMapper defaultObjektMapper;


    /**
     * Oppretter ulike varianter av ObjectMapper. Klient kan deretter velge hvilken som skal brukast ved å sette header i request.
     */
    public ObjectMapperResolver(final boolean featureFlagKodeverkAktiverKalkulusString) {
        this.baseObjektMapper = ObjectMapperFactory.createBaseObjectMapper();
        this.kodeverdiAlltidSomObjektMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkSerialiseringsvalg.OBJEKT_UTEN_NAVN,  true));
        this.kodeverdiAlltidSomStringMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkSerialiseringsvalg.KODE_STRING, false));
        this.sakKodeverdiStandardKalkulusKodeverdiStringMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkSerialiseringsvalg.STANDARD, false));
        // Bestemmer kva ObjectMapper som skal brukast når input header ikkje bestemmer det.
        // Bruker samme logikk som har vore pr no. Skal endrast til ønska framtidig standard når alle klienter har blitt
        // oppdatert til å handtere det.
        this.defaultObjektMapper = this.baseObjektMapper.copy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkSerialiseringsvalg.STANDARD, !featureFlagKodeverkAktiverKalkulusString));
    }

    public ObjectMapperResolver() {
        this(getFeatureFlagKodeverkAktiverKalkulusString());
    }

    private String getJsonSerializerOptionHeaderValue() {
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
            case "kodeverk-sak-standard-kalkulus-string" -> this.sakKodeverdiStandardKalkulusKodeverdiStringMapper;
            case "kodeverdi-string" -> this.kodeverdiAlltidSomStringMapper;
            case "kodeverdi-object" -> this.kodeverdiAlltidSomObjektMapper;
            case "base" -> this.baseObjektMapper;
            // Viss ingen gyldig header verdi, gjer det samme som før basert på feature flag.
            default -> this.defaultObjektMapper;
        };
    }

}
