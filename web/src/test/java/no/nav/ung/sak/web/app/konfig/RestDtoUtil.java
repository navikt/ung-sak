package no.nav.ung.sak.web.app.konfig;

import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import io.github.classgraph.ReferenceTypeSignature;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeSignature;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class RestDtoUtil {

    public static List<? extends Class<?>> hentAlleRestInputDtoerRekursivt() {
        ScanResult sr = new ClassGraph().enableAllInfo().scan();
        List<Class<? extends Annotation>> restMetodeAnnoteringer = List.of(GET.class, POST.class, PUT.class, DELETE.class, HEAD.class, PATCH.class);

        Set<ClassInfo> restTjenesteKlasser = finnAlleRestTjenester(restMetodeAnnoteringer, sr);
        Map<ClassInfo, String> dtoKlasserMedSporing = finnAlleInputDtoerDirektePåRestEndepunktene(restTjenesteKlasser, restMetodeAnnoteringer);
        Collection<ClassInfo> itereres = new ArrayList<>(dtoKlasserMedSporing.keySet());
        for (ClassInfo klasse : itereres) {
            populerKlasserMedSporing(sr, klasse, dtoKlasserMedSporing);
        }
        return dtoKlasserMedSporing.keySet().stream()
            .filter(Objects::nonNull)
            .map(ClassInfo::loadClass)
            .toList();
    }

    private static Map<ClassInfo, String> finnAlleInputDtoerDirektePåRestEndepunktene(Set<ClassInfo> restTjenesteKlasser, List<Class<? extends Annotation>> restMetodeAnnoteringer) {
        Map<ClassInfo, String> dtoKlasserMedSporing = new LinkedHashMap<>();
        for (ClassInfo restTjeneste : restTjenesteKlasser) {
            for (MethodInfo metode : restTjeneste.getMethodInfo()) {
                for (MethodParameterInfo metodeparameter : metode.getParameterInfo()) {
                    boolean erRestMetode = restMetodeAnnoteringer.stream().anyMatch(metode::hasAnnotation);
                    if (metodeparameter.isSynthetic() || !erRestMetode) {
                        continue;
                    }
                    TypeSignature typeSignature = metodeparameter.getTypeSignatureOrTypeDescriptor();
                    if (typeSignature instanceof ClassRefTypeSignature ctypeDescriptor) {
                        dtoKlasserMedSporing.put(ctypeDescriptor.getClassInfo(), restTjeneste.getSimpleName() + " " + metode.getName() + " " + metodeparameter.getName());
                        for (TypeArgument typeArgument : ctypeDescriptor.getTypeArguments()) {
                            ReferenceTypeSignature referenceTypeSignature = typeArgument.getTypeSignature();
                            if (referenceTypeSignature instanceof ClassRefTypeSignature classRefTypeSignature) {
                                dtoKlasserMedSporing.put(classRefTypeSignature.getClassInfo(), restTjeneste.getSimpleName() + " " + metode.getName() + " " + metodeparameter.getName());
                            } else {
                                throw new IllegalArgumentException("Håndterer ikke " + referenceTypeSignature);
                            }
                        }
                    } else if (typeSignature instanceof BaseTypeSignature baseTypeSignature) {
                        //primitive typer, ignorer
                    } else {
                        throw new IllegalArgumentException("Håndterer ikke " + typeSignature);
                    }
                }
            }
        }
        return dtoKlasserMedSporing;
    }

    private static Set<ClassInfo> finnAlleRestTjenester(List<Class<? extends Annotation>> restMetodeAnnoteringer, ScanResult sr) {
        Set<ClassInfo> restTjenesteKlasser = restMetodeAnnoteringer.stream()
            .flatMap(annotering -> sr.getClassesWithMethodAnnotation(annotering).stream())
            .filter(it -> it.getPackageName().startsWith("no.nav"))
            .collect(Collectors.toSet());
        return restTjenesteKlasser;
    }

    /**
     * Tar utgangspunkt i en DTO-klasse og fyller på med alle klasser denne referer til, og alle subklasser av denne
     */
    static void populerKlasserMedSporing(ScanResult sr, ClassInfo klasse, Map<ClassInfo, String> alle) {
        if (klasse == null
            || klasse.isEnum()
            || Properties.class.isAssignableFrom(klasse.loadClass()) //TODO kan forbedres ved å håndtere properties, brukes bare i prosesstask?
        ) {
            return;
        }

        Map<ClassInfo, String> nye = new LinkedHashMap<>();
        for (ClassInfo subklasse : sr.getSubclasses(klasse.loadClass())) {
            nye.put(subklasse, "Subklasse av " + klasse.getName());
        }
        for (FieldInfo field : klasse.getDeclaredFieldInfo()) {
            if (field.isStatic()) {
                continue;
            }
            TypeSignature typeSignature = field.getTypeSignatureOrTypeDescriptor();
            if (typeSignature instanceof ClassRefTypeSignature ctypeDescriptor) {
                nye.put(ctypeDescriptor.getClassInfo(), klasse.getName() + " " + field.getName());
                for (TypeArgument typeArgument : ctypeDescriptor.getTypeArguments()) {
                    ReferenceTypeSignature referenceTypeSignature = typeArgument.getTypeSignature();
                    if (referenceTypeSignature instanceof ClassRefTypeSignature classRefTypeSignature) {
                        nye.put(classRefTypeSignature.getClassInfo(), klasse.getName() + " " + field.getName());
                    } else {
                        throw new IllegalArgumentException("Håndterer ikke " + referenceTypeSignature);
                    }
                }
            } else if (typeSignature instanceof BaseTypeSignature baseTypeSignature) {
                //ignorer primitive typer
            } else {
                throw new IllegalArgumentException("Håndterer ikke " + typeSignature);
            }
        }
        for (ClassInfo fantesFraFør : alle.keySet()) {
            nye.remove(fantesFraFør);
        }
        alle.putAll(nye);
        //rekursivt for alle nyoppdagede klasser
        for (ClassInfo ny : nye.keySet()) {
            populerKlasserMedSporing(sr, ny, alle);
        }

    }
}
