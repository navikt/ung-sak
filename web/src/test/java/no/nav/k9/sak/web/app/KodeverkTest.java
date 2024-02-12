package no.nav.k9.sak.web.app;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.api.Kodeverdi;

public class KodeverkTest {

    @Test
    void name() throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        ClassLoader classLoader = KodeverkTest.class.getClassLoader();

        Field field = classLoader.getClass().getField("classes");
        field.setAccessible(true);

        Class<?> targetType = field.getType();
        Object objectValue = targetType.newInstance();
        Object value = field.get(objectValue);

        Class<?>[] klassene = (Class<?>[]) value;
        System.out.println("fant " + klassene.length + " klasser");

        for (Class<?> klasse : klassene) {
            if (Kodeverdi.class.isAssignableFrom(klasse)){
                System.out.println(klasse.getName());
            }

            if (klasse.isAssignableFrom(Kodeverdi.class)){
                System.out.println(klasse.getName() + " x");
            }
        }
    }
}
