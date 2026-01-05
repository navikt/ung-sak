package no.nav.ung.sak.web.app.konfig;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.app.jackson.IndexClasses;

@Execution(ExecutionMode.CONCURRENT)
public class RestApiInputValideringDtoTest extends RestApiTester {

    public static Stream<Arguments> provideArguments() {
        return RestDtoUtil.hentAlleRestInputDtoerRekursivt().stream().map(c -> Arguments.of(c)).collect(Collectors.toSet()).stream();
    }

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     */
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type(Class<?> dto) throws Exception {
        valider(dto);
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> UNNTATT_FRA_VALIDERING = new HashMap<>() {
        {

            put(PersonIdent.class, singletonList(emptyList()));

            put(boolean.class, singletonList(emptyList()));
            put(Boolean.class, singletonList(emptyList()));

            // LocalDate og LocalDateTime har egne deserializers
            put(LocalDate.class, singletonList(emptyList()));
            put(LocalDateTime.class, singletonList(emptyList()));

            // Enforces av UUID selv
            put(UUID.class, singletonList(emptyList()));

        }
    };

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> VALIDERINGSALTERNATIVER = new HashMap<>() {
        {
            put(String.class, asList(
                asList(Pattern.class, Size.class),
                asList(Pattern.class),
                singletonList(Digits.class)));
            put(Long.class, asList(
                asList(Min.class, Max.class),
                asList(Digits.class)));
            put(long.class, asList(
                asList(Min.class, Max.class),
                asList(Digits.class)));
            put(Integer.class, singletonList(
                asList(Min.class, Max.class)));
            put(int.class, singletonList(
                asList(Min.class, Max.class)));
            put(BigDecimal.class, asList(
                asList(Min.class, Max.class, Digits.class),
                asList(DecimalMin.class, DecimalMax.class, Digits.class)));

            putAll(UNNTATT_FRA_VALIDERING);
        }
    };

    private static List<List<Class<? extends Annotation>>> getVurderingsalternativer(Field field) {
        Class<?> type = field.getType();
        if (field.getType().isEnum()) {
            return Collections.singletonList(Collections.singletonList(Valid.class));
        } else if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            if (brukerGenerics(field)) {
                Type[] args = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                if (Arrays.stream(args).allMatch(UNNTATT_FRA_VALIDERING::containsKey)) {
                    return Collections.singletonList(List.of(Size.class));
                } else if (args.length == 1 && erKodeverk(args)) {
                    return Collections.singletonList(List.of(Valid.class, Size.class));
                }

            }
            return singletonList(List.of(Valid.class, Size.class));

        }
        return VALIDERINGSALTERNATIVER.get(type);
    }

    private static boolean erKodeverk(Type... args) {
        return Kodeverdi.class.isAssignableFrom(args[0].getClass()) || args[0].getClass().isEnum();
    }

    private static void valider(Class<?> klasse) throws URISyntaxException {
        if (erKodeverk(klasse)) {
            return;
        }
        if (erInterface(klasse)) {
            return;
        }
        if (UNNTATT_FRA_VALIDERING.containsKey(klasse)) {
            return;
        }

        ProtectionDomain protectionDomain = klasse.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            // system klasse
            return;
        }

        if (erImportertBibliotek(codeSource)) {
            // skip importerte biblioteker for nå
            return;
        }

        if (klasse.getAnnotation(Entity.class) != null || klasse.getAnnotation(MappedSuperclass.class) != null) {
            throw new AssertionError("Klassen " + klasse + " er en entitet, kan ikke brukes som DTO.");
        }


        for (Field field : getRelevantFields(klasse)) {
            if (field.getAnnotation(JsonIgnore.class) != null) {
                continue; // feltet blir hverken serialisert elle deserialisert, unntas fra sjekk
            }
            if (field.getAnnotation(JsonRawValue.class) != null) {
                continue; // feltet importeres/eksporteres rått - må valideres annen plass.
            }
            if (field.getType().isEnum()) {
                continue; // enum er OK
            }
            if (field.getType().isPrimitive()) {
                continue; // primitiv OK
            }
            if (erKodeverk(field.getType())) {
                continue; // antatt OK
            }
            if (isCollectionOrMapNotProperties(field)) {
                validerCollectionOrMap(field);
            } else {
                validerRiktigAnnotert(field);
            }
        }

        sjekkModifiersPåMetoderMedAssertTrueAssertFalse(klasse);
    }

    private static void validerCollectionOrMap(Field field) {
        // Ikke Valid før Collection/Map - men inne i generics-delen
        if (field.getAnnotatedType().getAnnotation(Valid.class) != null) {
            throw new AssertionError("Flytt annotering @Valid inn i List/Set/Collection/Map for feltet " + field + ".");
        }
        // Har Validering inne i generics-delen
        if (field.getAnnotatedType() instanceof AnnotatedParameterizedType annotatedParameterizedType) {
            var annotert = annotatedParameterizedType.getAnnotatedActualTypeArguments();
            for (var ann : annotert) {
                var påkrevdeAnnoteringer = utledPåkrevdeAnnoteringer(ann.getType());
                validerRiktigAnnotert(ann, påkrevdeAnnoteringer);
            }
            return;
        }
        throw new IllegalArgumentException("Feltet " + field + " har ikke påkrevde annoteringer.");
    }

    private static List<List<Class<? extends Annotation>>> utledPåkrevdeAnnoteringer(Type type) {
        var påkrevdeAnnoteringer = new ArrayList<List<Class<? extends Annotation>>>();
        if (erKodeverk(type)) {
            //påkrevdeAnnoteringer.add(List.of(ValidKodeverk.class));
        } else if (isCollectionOrMap(type)) {
            påkrevdeAnnoteringer.add(List.of(Size.class));
        } else if (VALIDERINGSALTERNATIVER.keySet().contains(type)) {
            påkrevdeAnnoteringer.addAll(VALIDERINGSALTERNATIVER.get(type));
        } else {
            påkrevdeAnnoteringer.add(List.of(Valid.class));
        }
        return påkrevdeAnnoteringer;
    }

    private static void validerRiktigAnnotert(AnnotatedType ann, List<List<Class<? extends Annotation>>> alternativer) {
        for (var alternativ : alternativer) {
            if (alternativ.isEmpty() || alternativ.stream().allMatch(ann::isAnnotationPresent)) {
                return;
            }
        }
        throw new IllegalArgumentException("Type " + ann + " har ikke påkrevde annoteringer: " + alternativer);
    }


    private static boolean isCollectionOrMapNotProperties(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()) && !Properties.class.isAssignableFrom(field.getType());
    }

    private static boolean isCollectionOrMap(Type type) {
        return Collection.class.isAssignableFrom(type.getClass()) || Map.class.isAssignableFrom(type.getClass());
    }


    private static void sjekkModifiersPåMetoderMedAssertTrueAssertFalse(Class<?> klasse) {
        for (Method metode : klasse.getDeclaredMethods()) {
            for (Class<? extends Annotation> annoteringsklasse : Set.of(AssertTrue.class, AssertFalse.class)) {
                if (Arrays.stream(metode.getDeclaredAnnotations()).anyMatch(annotation -> annotation.annotationType().equals(annoteringsklasse))) {
                    if (Modifier.isPrivate(metode.getModifiers())) {
                        throw new IllegalArgumentException(klasse + " har privat metode " + metode + " med " + annoteringsklasse + ", det fungerer ikke - bytt til annet scope");
                    }
                    if (Modifier.isStatic(metode.getModifiers())) {
                        throw new IllegalArgumentException(klasse + " har static metode " + metode + " med " + annoteringsklasse + ", det fungerer ikke");
                    }
                }
            }
        }
    }

    private static boolean erImportertBibliotek(CodeSource codeSource) {
        String codeSourceStr = codeSource.getLocation().toExternalForm();
        return !codeSourceStr.matches("^.*[/\\\\]kontrakt-[^/\\\\]+.jar$") && !codeSourceStr.matches("^.*[\\\\/]classes[\\\\/].*$");
    }

    private static boolean erKodeverk(Class<?> klasse) {
        return Kodeverdi.class.isAssignableFrom(klasse) || klasse.isEnum();
    }

    private static boolean erInterface(Class<?> klasse) {
        return klasse.isInterface();
    }

    private static Set<Class<?>> genericTypes(Field field) {
        Set<Class<?>> klasser = new HashSet<>();
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        for (Type t : type.getActualTypeArguments()) {
            klasser.add(t.getClass());
        }
        return klasser;
    }

    private static boolean brukerGenerics(Field field) {
        return field.getGenericType() instanceof ParameterizedType;
    }

    private static Set<Field> getRelevantFields(Class<?> klasse) {
        Set<Field> fields = new LinkedHashSet<>();
        while (!klasse.isPrimitive() && !klasse.getName().startsWith("java")) {
            fields.addAll(fjernStaticFields(List.of(klasse.getDeclaredFields())));
            klasse = klasse.getSuperclass();
        }
        return fields;
    }

    private static Collection<Field> fjernStaticFields(List<Field> fields) {
        return fields.stream().filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
    }

    private static void validerRiktigAnnotert(Field field) {
        var alternativer = getVurderingsalternativer(field);
        if (alternativer == null) {
            alternativer = List.of(List.of(Valid.class));
        }
        for (var alternativ : alternativer) {
            boolean harAlleAnnoteringerForAlternativet = true;
            for (Class<? extends Annotation> annotering : alternativ) {
                if (field.getAnnotation(annotering) == null) {
                    harAlleAnnoteringerForAlternativet = false;
                }
            }
            if (harAlleAnnoteringerForAlternativet) {
                return;
            }
        }
        throw new IllegalArgumentException("Feltet " + field + " har ikke påkrevde annoteringer: " + alternativer);
    }

}
