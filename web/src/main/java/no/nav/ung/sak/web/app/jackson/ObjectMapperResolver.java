package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.openapi.spec.utils.jackson.DynamicObjectMapperResolver;
import no.nav.openapi.spec.utils.jackson.OpenapiCompatObjectMapperModifier;

public class ObjectMapperResolver extends DynamicObjectMapperResolver {

    private final String JSON_SERIALIZER_ALLTID_SOM_STRING = "kodeverdi-string";
    private final String JSON_SERIALIZER_KALKULUS_SOM_STRING = "kodeverdi-kalkulus-string";

    private static ObjectMapper baseObjectMapper;

    private static ObjectMapper getBaseObjectMapperCopy() {
        if(baseObjectMapper == null) {
            baseObjectMapper = ObjectMapperFactory.createBaseObjectMapper();
        }
        return baseObjectMapper.copy();
    }

    // defaultObjektMapper brukast når input header for overstyring ikkje er satt.
    // Bruker samme logikk som har vore pr no. Det vil seie overstyring av Kalkulus Kodeverdi serialisering til objekt, så lenge ikkje feature flagg for string serialisering er aktivt.
    // Når alle klienter kan handtere at Kalkulus Kodeverdi kjem som string kan denne sannsynlegvis settast lik baseObjektMapper.
    private static ObjectMapper createDefaultObjectMapper() {
        return getBaseObjectMapperCopy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.INGEN));
    }

    /**
     * Oppretter ulike varianter av ObjectMapper. Klient kan deretter velge hvilken som skal brukast ved å sette header i request.
     */
    public ObjectMapperResolver() {
        super(createDefaultObjectMapper());
        final var overstyrKodeverdiAlltidSomStringMapper = getBaseObjectMapperCopy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.KODE_STRING));
        super.addObjectMapper(JSON_SERIALIZER_ALLTID_SOM_STRING, overstyrKodeverdiAlltidSomStringMapper);
        final var overstyrKalkulusKodeverdiSomStringMapper = getBaseObjectMapperCopy().registerModule(ObjectMapperFactory.createOverstyrendeKodeverdiSerializerModule(SakKodeverkOverstyringSerialisering.INGEN));
        super.addObjectMapper(JSON_SERIALIZER_KALKULUS_SOM_STRING, overstyrKalkulusKodeverdiSomStringMapper);
        // openaapiObjektMapper bør brukast viss ein ønsker at enums skal bli serialisert slik openapi spesifikasjon tilseier.
        final var openapiObjektMapper = OpenapiCompatObjectMapperModifier.withDefaultModifications().modify(getBaseObjectMapperCopy());
        super.addObjectMapper(JSON_SERIALIZER_OPENAPI, openapiObjektMapper);
    }

    // Brukt til testing
    public final ObjectMapper getDefaultObjectMapper() {
        return super.getDefaultObjectMapperCopy();
    }
    public final ObjectMapper getOverstyrKodeverdiAlltidSomStringMapper() {
        return super.getObjectMapperCopy(JSON_SERIALIZER_ALLTID_SOM_STRING).orElseThrow();
    }
    public final ObjectMapper getOpenapiObjektMapper() {
        return super.getObjectMapperCopy(DynamicObjectMapperResolver.JSON_SERIALIZER_OPENAPI).orElseThrow();
    }

    /**
     * Denne blir brukt for å bevare spesialtilfelle som har vore i koden.
     * Kan forhåpentlegvis unngå å bruke den ved overgang til ny serialisering (etter testing).
     * Viss denne kan fjernast kan superklasse brukast direkte, treng ikkje denne subklasse lenger.
     */
    private ObjectMapper overrideMapperForSøknad(Class<?> type, final ObjectMapper resolved) {
        // TODO Dette bør gjøres bedre slik at registrering av ObjectMapper gjøres lokalt i Rest-tjenesten.
        if (type.isAssignableFrom(Søknad.class)) {
            return JsonUtils.getObjectMapper();
        }
        return resolved;
    }

    /**
     * Resolver kva ObjectMapper som skal brukast for gitt type.
     * Denne er i tillegg dynamisk basert på header frå innkommande request. For at dette skal fungere må ein bruke
     * DynamicJacksonJsonProvider som skrur av caching av resolved ObjectMapper.
     */
    @Override
    protected ObjectMapper resolveMapper(Class<?> type, String serializerOption) {
        final var resolved = super.resolveMapper(type, serializerOption);
        if(serializerOption.equalsIgnoreCase(JSON_SERIALIZER_OPENAPI)) {
            return resolved;
        }
        // "legacy" object mappers har frå gammalt av ei spesialoverskriving:
        return this.overrideMapperForSøknad(type, resolved);
    }
}
