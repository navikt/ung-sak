package no.nav.k9.sak.web.app.jackson;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDto;
import no.nav.k9.sak.web.app.tjenester.RestImplementationClasses;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.omsorgspenger.mottak.OmsorgspengerSøknadInnsending;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.PleiepengerBarnSøknadInnsending;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;

@Provider
@Produces({ MediaType.APPLICATION_JSON })
public class JacksonJsonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper;

    /** Default instance for Jax-rs application. Genererer ikke navn som del av output for kodeverk. */
    public JacksonJsonConfig() {
        this(false);
    }

    public JacksonJsonConfig(boolean serialiserKodelisteNavn) {
        objectMapper = createObjectMapper(createModule(serialiserKodelisteNavn));
    }

    private ObjectMapper createObjectMapper(SimpleModule simpleModule) {
        var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(simpleModule);

        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        Collection<Class<?>> restClasses = new RestImplementationClasses().getImplementationClasses();

        Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for andre dtoer
        scanClasses.add(AvklarArbeidsforholdDto.class);
        scanClasses.add(VurderFaktaOmBeregningDto.class);
        scanClasses.add(OmsorgspengerSøknadInnsending.class);
        scanClasses.add(PleiepengerBarnSøknadInnsending.class);
        scanClasses.add(FrisinnSøknadInnsending.class);

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

    private static SimpleModule createModule(boolean serialiserKodelisteNavn) {
        SimpleModule module = new SimpleModule("VL-REST", new Version(1, 0, 0, null, null, null));

        addSerializers(module, serialiserKodelisteNavn);

        return module;
    }

    private static void addSerializers(SimpleModule module, boolean serialiserKodelisteNavn) {
        if(serialiserKodelisteNavn) {
            module.addSerializer(new KodelisteSerializer(serialiserKodelisteNavn));
        }
    }

    /**
     * Scan subtyper dynamisk fra WAR slik at superklasse slipper å deklarere @JsonSubtypes.
     */
    private static List<Class<?>> getJsonTypeNameClasses(URI classLocation) {
        IndexClasses indexClasses;
        indexClasses = IndexClasses.getIndexFor(classLocation);
        return indexClasses.getClassesWithAnnotation(JsonTypeName.class);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        // TODO Dette bør gjøres bedre slik at registrering av ObjectMapper gjøres lokalt i Rest-tjenesten.
        if (type.isAssignableFrom(Søknad.class)) {
            return JsonUtils.getObjectMapper();
        }
        return objectMapper;
    }

}
