package no.nav.ung.sak.web.server.typedresponse;

import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

/**
 * Kobler {@link TypedResponseFilter} til metoder som returnerer TypedResponse.
 */
@Provider
public class TypedResponseFeature implements DynamicFeature {

    private boolean declaresTypedResponse(final Class<?> cls) {
        if(TypedResponse.class.isAssignableFrom(cls)) {
            return true;
        }
        for (Class<?> iface : cls.getInterfaces()) {
            if (iface.equals(TypedResponse.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        final Class<?> respClass = resourceInfo.getResourceMethod().getReturnType();
        if(declaresTypedResponse(respClass)) {
            featureContext.register(TypedResponseFilter.class);
        }
    }
}
