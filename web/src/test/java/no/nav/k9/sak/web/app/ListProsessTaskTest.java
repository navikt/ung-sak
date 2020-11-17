package no.nav.k9.sak.web.app;

import java.lang.reflect.Modifier;
import java.util.TreeMap;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class ListProsessTaskTest {

    @Inject
    @Any
    private Instance<ProsessTaskHandler> handlers;

    @Test
    public void list_prosesstask_handlers() throws Exception {
        var tasks = new TreeMap<String, ProsessTaskHandler>();
        for (var pt : handlers) {
            if (!Modifier.isAbstract(pt.getClass().getModifiers())) {
                ProsessTask ann = pt.getClass().getAnnotation(ProsessTask.class);
                if (ann == null) {
                    System.out.println("Mangler Definisjon: " + pt.getClass().getName());
                } else {
                    tasks.put(ann.value(), pt);
                }
            }
        }

        tasks.forEach((v, p) -> System.out.println("ProsessTask: " + v + "[" + p.getClass().getName() + "]"));
    }
}
