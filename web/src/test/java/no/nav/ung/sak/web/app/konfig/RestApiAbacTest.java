package no.nav.ung.sak.web.app.konfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Fail.fail;

/**
 * Sjekker at alle REST endepunkt har definert tilgangskontroll konfigurert for ABAC (Attribute Based Access Control).
 */
public class RestApiAbacTest {

    public static Stream<Arguments> provideArguments() {
        return RestApiTester.finnAlleRestMetoder().stream().map(m -> Arguments.of(m))
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
            if (annotation.action() == BeskyttetRessursActionType.DUMMY) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for action()");
            }
            if (annotation.resource() == BeskyttetRessursResourceType.DUMMY) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk DUMMY-verdi for resource()");
            }
        }
    }

    private void assertAtIngenBrukerTommeEllerUgyldigeVerdierPåBeskyttetRessurs(Method metode) {
        Class<?> klasse = metode.getDeclaringClass();
        BeskyttetRessurs annotation = metode.getAnnotation(BeskyttetRessurs.class);
        if (annotation != null) {
            if (annotation.resource() == BeskyttetRessursResourceType.BEREGNINGSGRUNNLAG) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk BEREGNINGSGRUNNLAG-verdi for resource(). Brukes kun i kalkulus");
            }
            if (annotation.resource() == BeskyttetRessursResourceType.PDP) {
                fail(klasse.getSimpleName() + "." + metode.getName() + " Ikke bruk PDP-verdi for resource(). brukes kun i PDP");
            }
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
