package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.openapi.spec.utils.jackson.JsonParserPreProcessingDeserializerModifier;
import no.nav.openapi.spec.utils.jackson.ObjectToPropertyPreProcessor;
import no.nav.ung.kodeverk.LegacyKodeverdiSomObjektSerializer;
import no.nav.ung.kodeverk.KodeverdiSomStringSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.kontrakt.økonomi.tilbakekreving.VurderFeilutbetalingDto;
import no.nav.ung.sak.web.app.tjenester.RestImplementationClasses;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectMapperFactory {

    private static ObjectMapper baseObjectMapper;

    public static ObjectMapper getBaseObjectMapperCopy() {
        if(baseObjectMapper == null) {
            baseObjectMapper = ObjectMapperFactory.createBaseObjectMapper();
        }
        return baseObjectMapper.copy();
    }

    public static SimpleModule createOverstyrendeKodeverdiSerializerModule(final SakKodeverkOverstyringSerialisering sakKodeverkOverstyringSerialisering) {
        final SimpleModule module = new SimpleModule("KodeverdiSerialisering", new Version(1, 0, 0, null, null, null));

        if(sakKodeverkOverstyringSerialisering == SakKodeverkOverstyringSerialisering.KODE_STRING) {
            // Legger til overstyring av serialisering av Kodeverdi, til å bli kode string istadenfor
            // objekt som annotasjonane på disse pr no tilseier.
            // Denne kan fjernast når alle Kodeverdi typane sine annotasjoner er oppdatert slik at serialisering som standard blir ein rein string.
            module.addSerializer(new KodeverdiSomStringSerializer());

        } else if(sakKodeverkOverstyringSerialisering == SakKodeverkOverstyringSerialisering.LEGACY_OBJEKT) {
            // Legger til overstyring av serialisering av Kodeverdi enums til å serialisere til objekt uavhengig av annotasjon
            // (bortsett frå @LegacyKodeverdiJsonValue), slik at dei serialiserer til objekt sjølv om vi fjerner @JsonFormat(shape = object)
            // annotasjon på dei. Dette gjere det mulig å fjerne @JsonFormat som del av omskriving til @JsonValue på alle enums.
            module.addSerializer(new LegacyKodeverdiSomObjektSerializer());
        }
        // Støtter deserialisering frå Kodeverdi serialisert som json objekt uten at typen i seg sjølv støtter det.
        // Slik at vi kan skrive om Kodeverdi enums til å ha i utgangspunktet ha ein enkel @JsonValue basert deserialisering.
        final var kodeverdiDeserializerModifier = new JsonParserPreProcessingDeserializerModifier(new ObjectToPropertyPreProcessor(Kodeverdi.class, "kode"));
        module.setDeserializerModifier(kodeverdiDeserializerModifier);
        return module;
    }

    /**
     * Scan subtyper dynamisk fra WAR slik at superklasse slipper å deklarere @JsonSubtypes.
     */
    private static List<Class<?>> getJsonTypeNameClasses(URI classLocation) {
        IndexClasses indexClasses;
        indexClasses = IndexClasses.getIndexFor(classLocation);
        return indexClasses.getClassesWithAnnotation(JsonTypeName.class);
    }

    public static Collection<Class<?>> allJsonTypeNameClasses() {
        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        final Collection<Class<?>> restClasses = new RestImplementationClasses().getImplementationClasses();

        final Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        scanClasses.add(VurderFeilutbetalingDto.class);

        // avled code location fra klassene
        return scanClasses
            .stream()
            .map(c -> {
                try {
                    return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                }
            })
            .distinct()
            .flatMap(uri -> getJsonTypeNameClasses(uri).stream())
            .collect(Collectors.toUnmodifiableSet());
    }


    private static ObjectMapper createBaseObjectMapper() {
        final var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Registrer alle klasser med JsonTypeName annotasjon som subtyper i object mapper.
        // Slik at ein ikkje må deklarere disse manuelt som subtyper på alle superklasser.
        om.registerSubtypes(allJsonTypeNameClasses());

        return om;
    }

}
