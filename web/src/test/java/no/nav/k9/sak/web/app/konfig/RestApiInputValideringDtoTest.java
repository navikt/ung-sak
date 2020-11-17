package no.nav.k9.sak.web.app.konfig;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.annotation.Annotation;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.web.app.jackson.IndexClasses;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;

public class RestApiInputValideringDtoTest extends RestApiTester {

    public static Stream<Arguments> provideArguments() {
        return finnAlleDtoTyper().stream().map(c -> Arguments.of( c )).collect(Collectors.toSet()).stream();
    }

    /**
     * IKKE ignorer eller fjern denne testen, den sørger for at inputvalidering er i orden for REST-grensesnittene
     * <p>
     * Kontakt Team Humle hvis du trenger hjelp til å endre koden din slik at den går igjennom her
     */
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void alle_felter_i_objekter_som_brukes_som_inputDTO_skal_enten_ha_valideringsannotering_eller_være_av_godkjent_type(Class<?> dto) throws Exception {
        Set<Class<?>> validerteKlasser = new HashSet<>(); // trengs for å unngå løkker og unngå å validere samme klasse flere multipliser dobbelt
        validerRekursivt(validerteKlasser, dto, null);
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> UNNTATT_FRA_VALIDERING = new HashMap<Class, List<List<Class<? extends Annotation>>>>() {
        {

            put(PersonIdent.class, singletonList(emptyList()));

            put(boolean.class, singletonList(emptyList()));
            put(Boolean.class, singletonList(emptyList()));

            // LocalDate og LocalDateTime har egne deserializers
            put(LocalDate.class, singletonList(emptyList()));
            put(LocalDateTime.class, singletonList(emptyList()));

            // Enforces av UUID selv
            put(UUID.class, singletonList(emptyList()));

            // Sjekkes av separat validator
            put(PleiepengerBarnSøknad.class, singletonList(emptyList()));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Map<Class, List<List<Class<? extends Annotation>>>> VALIDERINGSALTERNATIVER = new HashMap<Class, List<List<Class<? extends Annotation>>>>() {
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
        return Kodeverdi.class.isAssignableFrom((Class<?>) args[0]);
    }

    private static Set<Class<?>> finnAlleDtoTyper() {
        Set<Class<?>> klasser = new TreeSet<>(Comparator.comparing(Class::getName));
        for (Method method : finnAlleRestMetoder()) {
            klasser.addAll(List.of(method.getParameterTypes())); // sjekker input parameter typer til rest tjeneste
            klasser.add(method.getReturnType()); // sjekker return type også
            for (Type type : method.getGenericParameterTypes()) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType genericTypes = (ParameterizedType) type;
                    for (Type gen : genericTypes.getActualTypeArguments()) {
                        if (!(gen instanceof WildcardType)) {
                            klasser.add((Class<?>) gen);
                        } else {
                            System.err.println("Fikk wilcard type param: " + method + ": " + gen); // NOSONAR
                        }
                    }
                }
            }
        }
        Set<Class<?>> filtreteParametre = new TreeSet<>(Comparator.comparing(Class::getName));
        for (Class<?> klasse : klasser) {
            if (klasse.getName().startsWith("java")) {
                // ikke sjekk nedover i innebygde klasser, det skal brukes annoteringer på tidligere tidspunkt
                continue;
            }
            filtreteParametre.add(klasse);
        }
        return filtreteParametre;
    }

    private static void validerRekursivt(Set<Class<?>> besøkteKlasser, Class<?> klasse, Class<?> forrigeKlasse) throws URISyntaxException {
        if (erKodeverk(klasse)) {
            return;
        }
        if (besøkteKlasser.contains(klasse)) {
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

        besøkteKlasser.add(klasse);
        if (klasse.getAnnotation(Entity.class) != null || klasse.getAnnotation(MappedSuperclass.class) != null) {
            throw new AssertionError("Klassen " + klasse + " er en entitet, kan ikke brukes som DTO. Brukes i " + forrigeKlasse);
        }

        URL klasseLocation = codeSource.getLocation();
        for (Class<?> subklasse : IndexClasses.getIndexFor(klasseLocation.toURI()).getSubClassesWithAnnotation(klasse, JsonTypeName.class)) {
            validerRekursivt(besøkteKlasser, subklasse, forrigeKlasse);
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
            } else {
                validerRiktigAnnotert(field);
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
            }
            if (brukerGenerics(field)) {
                validerRekursivt(besøkteKlasser, field.getType(), forrigeKlasse);
                for (Class<?> klazz : genericTypes(field)) {
                    validerRekursivt(besøkteKlasser, klazz, forrigeKlasse);
                }
            }
        }
    }

    private static boolean erImportertBibliotek(CodeSource codeSource) {
        String codeSourceStr = codeSource.getLocation().toExternalForm();
        return !codeSourceStr.matches("^.*[/\\\\]kontrakt-[^/\\\\]+.jar$") && !codeSourceStr.matches("^.*[\\\\/]classes[\\\\/].*$");
    }

    private static boolean erKodeverk(Class<?> klasse) {
        return Kodeverdi.class.isAssignableFrom(klasse);
    }

    private static Set<Class<?>> genericTypes(Field field) {
        Set<Class<?>> klasser = new HashSet<>();
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        for (Type t : type.getActualTypeArguments()) {
            klasser.add((Class<?>) t);
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
