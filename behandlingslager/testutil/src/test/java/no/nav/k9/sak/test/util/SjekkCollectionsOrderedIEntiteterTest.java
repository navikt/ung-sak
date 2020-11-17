package no.nav.k9.sak.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.ManagedType;

import org.junit.Assume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.db.util.Databaseskjemainitialisering;
import no.nav.k9.sak.db.util.DatasourceConfiguration;
import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;

/** Lagt til web for å sjekke orm filer fra alle moduler. */
public class SjekkCollectionsOrderedIEntiteterTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        Databaseskjemainitialisering.settJdniOppslag();
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() throws Exception {

        Set<Class<?>> baseEntitetSubklasser = getEntityClasses(BaseEntitet.class::isAssignableFrom);
        Set<Class<?>> entityKlasser = getEntityClasses(c -> c.isAnnotationPresent(Entity.class));
        Map<String, Object[]> params = new LinkedHashMap<>();

        for (Class<?> c : baseEntitetSubklasser) {
            params.put(c.getName(), new Object[]{c.getSimpleName(), c});
        }
        assertThat(params).isNotEmpty();

        for (Class<?> c : entityKlasser) {
            params.put(c.getName(), new Object[]{c.getSimpleName(), c});
        }
        assertThat(params).isNotEmpty();

        return params.values();
    }

    public static Stream<Arguments> provideArguments() {

        List<Arguments> params = new LinkedList<Arguments>();

        Set<Class<?>> baseEntitetSubklasser = getEntityClasses(BaseEntitet.class::isAssignableFrom);
        for (Class<?> c : baseEntitetSubklasser) {
            params.add(Arguments.of(c.getName(), c));
        }

        Set<Class<?>> entityKlasser = getEntityClasses(c -> c.isAnnotationPresent(Entity.class));
        for (Class<?> c : entityKlasser) {
            params.add(Arguments.of(c.getName(), c));
        }

        return params.stream();
    }

    public static Set<Class<?>> getEntityClasses(Predicate<Class<?>> filter) {
        Set<ManagedType<?>> managedTypes = entityManagerFactory.getMetamodel().getManagedTypes();
        return managedTypes.stream().map(javax.persistence.metamodel.Type::getJavaType).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(filter).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_alle_lister_er_ordered(String name, Class<?> entityClass) throws Exception {
        for (Field f : entityClass.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(f.getType())) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    ParameterizedType paramType = (ParameterizedType) f.getGenericType();
                    Class<?> cls = (Class<?>) paramType.getActualTypeArguments()[0];
                    Assume.assumeTrue(IndexKey.class.isAssignableFrom(cls));
                    assertThat(IndexKey.class).as(f + " definerer en liste i " + name).isAssignableFrom(cls);
                }
            }
        }
    }

}
