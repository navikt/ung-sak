package no.nav.ung.sak;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
public class SjekkGyldigTaskNavnTest {

    @Inject
    @Any
    Instance<ProsessTaskHandler> batchTaskHandlers;

    @Test
    void alle_prosesstask_implementasjoner_skal_ha_tasknavn_ikke_over_50_tegn() {
        List<String> feil = new ArrayList<>();

        var tasker = batchTaskHandlers.stream().map(this::finnKlasseFor).toList();

        for (var clazz : tasker) {
            ProsessTask annotation = clazz.getAnnotation(ProsessTask.class);

            if (annotation != null) {
                String taskName = annotation.value();
                if (taskName.length() > 50) {
                    feil.add(String.format("Klasse %s har tasknavn '%s' med lengde %d (må være <= 50 tegn)",
                        clazz.getSimpleName(), taskName, taskName.length()));
                }
            }
        }

        assertThat(feil)
            .withFailMessage("Følgende ProsessTask-implementasjoner har tasknavn som er for lange:\n%s",
                String.join("\n", feil))
            .isEmpty();
    }

    private Class<? extends ProsessTaskHandler> finnKlasseFor(ProsessTaskHandler taskHandler) {
        if (taskHandler instanceof TargetInstanceProxy) {
            return ((TargetInstanceProxy) taskHandler).weld_getTargetClass();
        }
        return taskHandler.getClass();
    }

}
