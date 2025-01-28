package no.nav.ung.fordel.util;

import jakarta.enterprise.inject.spi.CDI;
import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.UnboundLiteral;

import java.util.function.Supplier;

/**
 * Kjør angitt funksjon med RequestScope aktivt.
 */
public final class RequestContextHandler {

    private RequestContextHandler() {
        // hidden ctor
    }

    public static void doWithRequestContext(Runnable runnable) {
        doReturnWithRequestContext(() -> {
            runnable.run();
            return null;
        });
    }

    public static <V> V doReturnWithRequestContext(Supplier<V> supplier) {

        RequestContext requestContext = CDI.current().select(RequestContext.class, UnboundLiteral.INSTANCE).get();
        if (requestContext.isActive()) {
            return supplier.get();
        } else {

            try {
                requestContext.activate();
                return supplier.get();
            } finally {
                requestContext.invalidate();
                requestContext.deactivate();
            }
        }
    }

}
