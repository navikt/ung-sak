package no.nav.k9.sak.web.app.konfig;

import static org.assertj.core.api.Fail.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.k9.abac.BeskyttetRessursKoder;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Sjekker at alle REST endepunkt har definert tilgangskontroll konfigurert for ABAC (Attribute Based Access Control).
 */
public class RestApiAbacTest {

    public static Stream<Arguments> provideArguments() {
        return RestApiTester.finnAlleRestMetoder().stream().map(m -> Arguments.of( m ))
            .collect(Collectors.toList()).stream();
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void test_at_alle_restmetoder_er_annotert_med_BeskyttetRessurs(Method restMethod) throws Exception {
        if (restMethod.getAnnotation(BeskyttetRessurs.class) == null) {
            throw new AssertionError("Mangler @" + BeskyttetRessurs.class.getSimpleName() + "-annotering på " + restMethod);
        }
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_at_ingen_metoder_er_annotert_med_dummy_verdier(Method restMethod) throws IllegalAccessException {
        assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(restMethod);
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_at_ingen_metoder_er_ressurs_annotert_med_tomme_eller_ugyldige_verdier(Method restMethod) throws IllegalAccessException {
        assertAtIngenBrukerTommeEllerUgyldigeVerdierPåBeskyttetRessurs(restMethod);
    }

    /**
     * IKKE ignorer denne testen, helper til med at input til tilgangskontroll blir riktig
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her *
     */
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void test_at_alle_input_parametre_til_restmetoder_implementer_AbacDto_eller_spesifiserer_AbacDataSupplier(Method restMethod) throws Exception {
        String feilmelding = "Parameter på %s.%s av type %s må implementere " + AbacDto.class.getSimpleName()
            + ", eller være annotatert med @TilpassetAbacAttributt.\n";
        StringBuilder feilmeldinger = new StringBuilder();

        Parameter[] parameters = restMethod.getParameters();
        var parameterAnnotations = restMethod.getParameterAnnotations();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            if (IgnorerteInputTyper.ignore(parameterType)) {
                continue;
            }
            Annotation[] paramAnnotation = parameterAnnotations[i];
            if (Collection.class.isAssignableFrom(parameterType)) {
                ParameterizedType type = (ParameterizedType) parameter.getParameterizedType();
                @SuppressWarnings("rawtypes")
                Class<?> aClass = (Class) (type.getActualTypeArguments()[0]);
                if (!harAbacKonfigurasjon(paramAnnotation, aClass)) {
                    feilmeldinger
                        .append(String.format(feilmelding, restMethod.getDeclaringClass().getSimpleName(), restMethod.getName(), aClass.getSimpleName()));
                }
            } else {
                if (!harAbacKonfigurasjon(paramAnnotation, parameterType)) {
                    feilmeldinger.append(
                        String.format(feilmelding, restMethod.getDeclaringClass().getSimpleName(), restMethod.getName(), parameterType.getSimpleName()));
                }
            }
        }
        if (feilmeldinger.length() > 0) {
            throw new AssertionError("Følgende inputparametre til REST-tjenester mangler AbacDto-impl\n" + feilmeldinger);
        }
    }

    private boolean harAbacKonfigurasjon(Annotation[] parameterAnnotations, Class<?> parameterType) {
        var ret = AbacDto.class.isAssignableFrom(parameterType) || IgnorerteInputTyper.ignore(parameterType);
        if (!ret) {
            ret = List.of(parameterAnnotations).stream().anyMatch(a -> TilpassetAbacAttributt.class.equals(a.annotationType()));
        }
        return ret;
    }

    private void assertAtIngenBrukerDummyVerdierPåBeskyttetRessurs(Method metode) {
        Class<?> klasse = metode.getDeclaringClass();
        BeskyttetRessurs annotation = metode.getAnnotation(BeskyttetRessurs.class);
        if (annotation != null) {
            if (annotation.action() == BeskyttetRessursActionAttributt.DUMMY) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for "
                    + BeskyttetRessursActionAttributt.class.getSimpleName());
            } else if (annotation.property() != null && !"".equals(annotation.property())) {
                return; // ok
            } else if (annotation.resource().isEmpty() &&
                annotation.ressurs() == BeskyttetRessursResourceAttributt.DUMMY) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for "
                    + BeskyttetRessursResourceAttributt.class.getSimpleName());
            }
        }
    }

    private void assertAtIngenBrukerTommeEllerUgyldigeVerdierPåBeskyttetRessurs(Method metode) {
        Class<?> klasse = metode.getDeclaringClass();
        BeskyttetRessurs annotation = metode.getAnnotation(BeskyttetRessurs.class);
        final List<String> konstanter = Arrays.stream(BeskyttetRessursKoder.class.getDeclaredFields())
            .filter(it -> Modifier.isStatic(it.getModifiers()) && Modifier.isFinal(it.getModifiers()))
            .map(it -> extractValueFromField(it))
            .collect(Collectors.toList());

        if (annotation != null && !annotation.property().isEmpty()) {
            return; // ok
        }

        if (annotation != null && annotation.ressurs() == BeskyttetRessursResourceAttributt.DUMMY) {
            if (annotation.resource().isEmpty()) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk tom-verdi for "
                    + BeskyttetRessursResourceAttributt.class.getSimpleName());
            }

            if (!konstanter.contains(annotation.resource())) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Bruk verdi fra kodeliste for "
                    + BeskyttetRessursResourceAttributt.class.getSimpleName());
            }
        }
    }

    private String extractValueFromField(Field field) {
        try {
            return (String) field.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Disse typene slipper naturligvis krav om impl av {@link AbacDto}
     */
    enum IgnorerteInputTyper {
        BOOLEAN(Boolean.class.getName()),
        REQUEST(Request.class.getName()),
        RESPONSE(Response.class.getName()),
        URIINFO(UriInfo.class.getName()),
        SERVLETREQ(HttpServletRequest.class.getName()),
        SERVLETRES(HttpServletResponse.class.getName());

        private String className;

        IgnorerteInputTyper(String className) {
            this.className = className;
        }

        static boolean ignore(Class<?> klasse) {
            return Arrays.stream(IgnorerteInputTyper.values()).anyMatch(e -> e.className.equals(klasse.getName()));
        }
    }

}
