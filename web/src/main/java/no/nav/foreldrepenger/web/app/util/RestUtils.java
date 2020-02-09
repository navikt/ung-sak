package no.nav.foreldrepenger.web.app.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import no.nav.foreldrepenger.web.app.ApplicationConfig;
import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;
import no.nav.foreldrepenger.web.server.jetty.JettyWebKonfigurasjon;
import no.nav.k9.sak.kontrakt.ResourceLink;

public class RestUtils {

    /**
     * If the class have the
     */
    public static String getClassAnnotationValue(Class<?> aClass, @SuppressWarnings("rawtypes") Class annotationClass, String name) {
        @SuppressWarnings("unchecked")
        Annotation aClassAnnotation = aClass.getAnnotation(annotationClass);
        if (aClassAnnotation != null) {
            Class<? extends Annotation> type = aClassAnnotation.annotationType();
            for (Method method : type.getDeclaredMethods()) {
                try {
                    Object value = method.invoke(aClassAnnotation, new Object[0]);
                    if (method.getName().equals(name)) {
                        return value.toString();
                    }

                } catch (InvocationTargetException e) {

                } catch (IllegalAccessException e) {

                }
            }
        }
        return null;
    }

    public static String getApiPath() {
        String contextPath = JettyWebKonfigurasjon.CONTEXT_PATH;
        String apiUri = ApplicationConfig.API_URI;
        return contextPath + apiUri;
    }

    public static String getApiPath(String segment) {
        return getApiPath() + segment;
    }

    public static Collection<ResourceLink> getRoutes() {
        Set<ResourceLink> routes = new HashSet<>();
        Collection<Class<?>> restClasses = new RestImplementationClasses().getImplementationClasses();
        for (Class<?> aClass : restClasses) {
            String pathFromClass = getClassAnnotationValue(aClass, Path.class, "value");
            Method[] methods = aClass.getMethods();
            for (Method aMethod : methods) {
                ResourceLink.HttpMethod method = null;
                if (aMethod.getAnnotation(POST.class) != null) {
                    method = ResourceLink.HttpMethod.POST;
                }
                if (aMethod.getAnnotation(GET.class) != null) {
                    method = ResourceLink.HttpMethod.GET;
                }
                if (aMethod.getAnnotation(PUT.class) != null) {
                    method = ResourceLink.HttpMethod.PUT;
                }
                if (aMethod.getAnnotation(DELETE.class) != null) {
                    method = ResourceLink.HttpMethod.DELETE;
                }
                if (method != null) {
                    String pathFromMethod = "";
                    if (aMethod.getAnnotation(Path.class) != null) {
                        pathFromMethod = aMethod.getAnnotation(Path.class).value();
                    }
                    ResourceLink resourceLink = new ResourceLink(getApiPath() + pathFromClass + pathFromMethod, aMethod.getName(), method);
                    routes.add(resourceLink);
                }
            }
        }
        return routes;
    }

}
