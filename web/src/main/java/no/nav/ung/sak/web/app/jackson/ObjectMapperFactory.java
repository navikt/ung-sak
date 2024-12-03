package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.ung.kodeverk.KodeverdiSomStringSerializer;
import no.nav.ung.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.ung.sak.web.app.tjenester.RestImplementationClasses;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ObjectMapperFactory {

    public static SimpleModule createOverstyrendeKodeverdiSerializerModule(final SakKodeverkOverstyringSerialisering sakKodeverkOverstyringSerialisering) {
        final SimpleModule module = new SimpleModule("KodeverdiSerialisering", new Version(1, 0, 0, null, null, null));

        if(sakKodeverkOverstyringSerialisering == SakKodeverkOverstyringSerialisering.KODE_STRING) {
            // Legger til overstyring av serialisering av k9.kodeverk.api.Kodeverdi, til å bli kode string istadenfor
            // objekt som annotasjonane på disse pr no tilseier.
            // Denne kan fjernast når alle Kodeverdi typane sine annotasjoner er oppdatert slik at serialisering som standard blir ein rein string.
            module.addSerializer(new KodeverdiSomStringSerializer());
        }
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

    public static ObjectMapper createBaseObjectMapper() {
        final var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        Collection<Class<?>> restClasses = new RestImplementationClasses().getImplementationClasses();

        Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for andre dtoer
        scanClasses.add(AvklarArbeidsforholdDto.class);

        // avled code location fra klassene
        scanClasses
            .stream()
            .map(c -> {
                try {
                    return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                }
            })
            .distinct()
            .forEach(uri -> om.registerSubtypes(getJsonTypeNameClasses(uri)));
        return om;
    }

}
