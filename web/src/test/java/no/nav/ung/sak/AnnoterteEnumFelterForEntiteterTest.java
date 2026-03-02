package no.nav.ung.sak;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.db.util.JpaExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
public class AnnoterteEnumFelterForEntiteterTest {

    @Test
    void alle_enum_felter_i_entiteter_skal_være_annotert_med_convert() {
        // for å unngå at hibernate lagrer enumeral idsf kode
        // vi ønsker å unngå å lagre enums som enumerals da det gir risiko for feil ved endring av rekkefølge i enum

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("pu-default");
        Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
        List<String> mangler = new ArrayList<>();
        for (EntityType<?> entity : entities) {
            Class<?> javaType = entity.getJavaType();
            if (javaType.isAnnotationPresent(Entity.class)) {
                Arrays.stream(javaType.getDeclaredFields())
                    .filter(field -> field.getType().isEnum())
                    .forEach(field -> {
                        Enumerated enumerated = field.getAnnotation(Enumerated.class);
                        Convert convert = field.getAnnotation(Convert.class);
                        Transient transientFelt = field.getAnnotation(Transient.class);
                        boolean manglerAnotering = convert == null && enumerated == null && transientFelt == null;
                        if (manglerAnotering) {
                            mangler.add("Feltet " + field.getName() + " i entiteten " + javaType.getSimpleName() + " er av enum type og må derfor annoteres med @Convert eller @Enumerated(EnumType.STRING)");
                        }
                        boolean feilAnnotering = enumerated != null && enumerated.value() == EnumType.ORDINAL;
                        if (feilAnnotering) {
                            mangler.add("Feltet " + field.getName() + " i entiteten " + javaType.getSimpleName() + " er av enum type og må derfor annoteres med @Convert eller @Enumerated(EnumType.STRING)");
                        }
                    });
            }
        }

        Assertions.assertThat(mangler).isEmpty();
    }
}


