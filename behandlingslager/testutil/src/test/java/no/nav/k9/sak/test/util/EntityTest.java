package no.nav.k9.sak.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;
import javax.persistence.Table;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.db.util.DatasourceConfiguration;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

/** Sjekker alle entiteter er mappet korrekt.  Ligger i web slik at den fanger alle orm filer lagt i ulike moduler. */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class EntityTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        // Kan ikke skrus på nå - trigger på CHAR kolonner som kunne vært VARCHAR.  Må fikses først
        //System.setProperty("hibernate.hbm2ddl.auto", "validate");
        try {
            // trenger å konfigurere opp jndi etc.
            DBConnectionProperties connectionProperties = DBConnectionProperties.finnDefault(DatasourceConfiguration.UNIT_TEST.get()).get();
            DatabaseStøtte.settOppJndiForDefaultDataSource(Collections.singletonList(connectionProperties));
        } catch (FileNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    @Inject
    private EntityManager entityManager;

    @AfterAll
    public static void teardown() {
        System.clearProperty("hibernate.hbm2ddl.auto");
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

    private static boolean isDoubleOrFloat(Class<?> javaType) {
        return javaType == Double.class || javaType == Float.class || (javaType.isPrimitive() && (javaType == Double.TYPE || javaType == Float.TYPE));
    }

    private static <V extends Annotation> V getInheritedAnnotation(Class<?> cls, Class<V> ann) {
        V res = null;
        while (res == null && cls != Object.class) {
            res = cls.getDeclaredAnnotation(ann);
            cls = cls.getSuperclass();
        }
        return res;
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void skal_ha_registrert_alle_entiteter_i_orm_xml(String name, Class<?> entityClass) {
        try {
            entityManagerFactory.getMetamodel().managedType(entityClass);
        } catch (IllegalArgumentException e) {
            assertThat(e).as("Er ikke registrert i orm, må ryddes fra koden: " + name).isNull(); // Skal alltid feile, kun for å utvide melding
            throw e;
        }
    }

    @Disabled("FIXME er ikke portet til Postgres")
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_felt_mapping_primitive_felt_i_entiteter_må_ha_not_nullable_i_db(String name, Class<?> entityClass) throws Exception {
        ManagedType<?> managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        for (Attribute<?, ?> att : managedType.getAttributes()) {
            Class<?> javaType = att.getJavaType();
            if (javaType.isPrimitive()) {

                Member member = att.getJavaMember();
                Field field = member.getDeclaringClass().getDeclaredField(member.getName());
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                String tableName = getInheritedAnnotation(entityClass, Table.class).name();
                String columnName = field.getDeclaredAnnotation(Column.class).name();
                String singleResult = getNullability(tableName, columnName);

                if (singleResult != null) {
                    String warn = "Primitiv " + member + " kan ha null i db. Kan medføre en smell ved lasting";
                    assertThat(singleResult).as(warn).isEqualTo("N");
                } else {
                    // forventer noe Dvh stuff som er Ok
                    assertThat(entityClass.getName()).endsWith("Dvh");
                }
            }
        }
    }

    @Disabled("FIXME er ikke portet til Postgres")
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_felt_ikke_primitive_wrappere_kan_ikke_være_not_nullable_i_db(String name, Class<?> entityClass) throws Exception {
        ManagedType<?> managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        if (Modifier.isAbstract(entityClass.getModifiers())) {
            return;
        }

        for (Attribute<?, ?> att : managedType.getAttributes()) {
            Member member = att.getJavaMember();
            Field field = member.getDeclaringClass().getDeclaredField(member.getName());

            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            Id id = field.getDeclaredAnnotation(Id.class);
            if (id != null) {
                continue;
            }

            String tableName = getTableName(field, entityClass);
            Column column = field.getDeclaredAnnotation(Column.class);
            JoinColumn joinColumn = field.getDeclaredAnnotation(JoinColumn.class);
            if (column == null && joinColumn == null) {
                continue;
            }
            String columnName = column != null ? column.name() : joinColumn.name();
            boolean nullable = column != null ? column.nullable() : joinColumn.nullable();
            String singleResult = getNullability(tableName, columnName);

            String warn = "Felt " + member
                + " kan ikke ha null i db. Kan medføre en smell ved skriving. Bedre å bruke primitiv hvis kan (husk sjekk med innkommende kilde til data)";
            if (nullable) {
                assertThat(singleResult).as(warn).isEqualTo("Y");
            } else {
                assertThat(singleResult).as(warn).isEqualTo("N");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getNullability(String tableName, String columnName) {
        List<String> result = entityManager.createNativeQuery(
            "SELECT NULLABLE FROM ALL_TAB_COLS WHERE COLUMN_NAME=upper(:col) AND TABLE_NAME=upper(:table) AND OWNER=SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')")
            .setParameter("table", tableName)
            .setParameter("col", columnName)
            .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    private String getTableName(Field field, Class<?> entityClass) {
        Class<?> clazz = entityClass;
        if (field.getDeclaredAnnotation(OneToMany.class) != null) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Type type = parameterizedType.getActualTypeArguments()[0];
            clazz = (Class<?>) type;
        }
        return getInheritedAnnotation(clazz, Table.class).name();
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void sjekk_felt_ikke_er_Float_eller_Double(String name, Class<?> entityClass) throws Exception {
        ManagedType<?> managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        for (Attribute<?, ?> att : managedType.getAttributes()) {

            Class<?> javaType = att.getJavaType();

            if (isDoubleOrFloat(javaType)) {

                Member member = att.getJavaMember();
                Field field = member.getDeclaringClass().getDeclaredField(member.getName());
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                String warn = "Primitiv wrapper (Float, Double) " + member
                    + " bør ikke brukes for felt som mappes til db.  Vil gi IEEE754 avrundingsfeil";

                assertThat(member).as(warn).isNull();
            }
        }
    }

}
