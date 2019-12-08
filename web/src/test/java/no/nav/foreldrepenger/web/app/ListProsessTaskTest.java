package no.nav.foreldrepenger.web.app;

import java.lang.reflect.Modifier;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class ListProsessTaskTest {

    @Inject
    @Any
    private Instance<ProsessTaskHandler> handlers;

    @Test
    public void list_prosesstask_handlers() throws Exception {
        for (var pt : handlers) {
            if (!Modifier.isAbstract(pt.getClass().getModifiers())) {
                ProsessTask ann = pt.getClass().getAnnotation(ProsessTask.class);
                if (ann == null) {
                    System.out.println("Mangler Definisjon: " + pt.getClass().getName());
                } else {
                    System.out.println("ProsessTask: " + ann.value());
                }
            }
        }
    }
}
