package no.nav.foreldrepenger.web.app.konfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RestApiInputValideringAnnoteringTest extends RestApiTester {

    @Parameterized.Parameters(name = "Validerer Dto - {0}")
    public static Collection<Object[]> getRestMetoder() {
        return RestApiTester.finnAlleRestMetoder().stream().map(m -> new Object[] { m.getDeclaringClass().getName() + "#" + m.getName(), m })
            .collect(Collectors.toList());
    }

    private Method restMethod;
    @SuppressWarnings("unused")
    private String name;

    private Function<Method, String> printKlasseOgMetodeNavn = (method -> String.format("%s.%s", method.getDeclaringClass(), method.getName()));

    public RestApiInputValideringAnnoteringTest(String name, Method restMethod) {
        this.name = name;
        this.restMethod = restMethod;

    }

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @Test
    public void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type() throws Exception {
        for (int i = 0; i < restMethod.getParameterCount(); i++) {
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
