package no.nav.k9.sak.web.app.konfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.ws.rs.core.Context;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RestApiInputValideringAnnoteringTest extends RestApiTester {

    public static Stream<Arguments> provideArguments() {
        return RestApiTester.finnAlleRestMetoder().stream().map(m -> Arguments.of( m ))
            .collect(Collectors.toList()).stream();
    }

    private Function<Method, String> printKlasseOgMetodeNavn = (method -> String.format("%s.%s", method.getDeclaringClass(), method.getName()));

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type(Method restMethod) throws Exception {
        for (int i = 0; i < restMethod.getParameterCount(); i++) {
            if(restMethod.getParameters()[i].getType().isEnum()) {
                continue;
            }
            assertThat(restMethod.getParameterTypes()[i].isAssignableFrom(String.class)).as(
                "REST-metoder skal ikke har parameter som er String eller mer generelt. Bruk DTO-er og valider. " + printKlasseOgMetodeNavn.apply(restMethod))
                .isFalse();
            assertThat(isRequiredAnnotationPresent(restMethod.getParameters()[i]))
                .as("Alle parameter for REST-metoder skal være annotert med @Valid. Var ikke det for " + printKlasseOgMetodeNavn.apply(restMethod))
                .withFailMessage("Fant parametere som mangler @Valid annotation '" + restMethod.getParameters()[i].toString() + "'").isTrue();
        }
    }

    private boolean isRequiredAnnotationPresent(Parameter parameter) {
        final Valid validAnnotation = parameter.getAnnotation(Valid.class);
        if (validAnnotation == null) {
            final Context contextAnnotation = parameter.getAnnotation(Context.class);
            return contextAnnotation != null;
        }
        return true;
    }

}
