package no.nav.k9.sak.web.app;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Denne klassen justerer generert openapi spesifikasjon når rest endepunkt returnerer Optional<T> type, altså eit resultat
 * som kan vere null eller ein gitt type. Modifiserer då generert openapi spesifikasjon slik at respons definisjon blir
 * satt som "nullable": true. For at dette skal fungere må også den opprinnelege respons type $ref flyttast inn i ein "allOf"
 * array.
 *
 * Gjere denne modifikasjon berre for responser av type "default" eller "200" pr no. Så ein kan med annotasjoner ha andre
 * respons koder som returnerer konkrete andre typer som ikkje er nullable, feks ved feil.
 */
public class OptionalResponseReader extends Reader {

    @Override
    protected Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters, Produces methodProduces, Produces classProduces, Consumes methodConsumes, Consumes classConsumes, List<SecurityRequirement> classSecurityRequirements, Optional<ExternalDocumentation> classExternalDocs, Set<String> classTags, List<Server> classServers, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, JsonView jsonViewAnnotation, ApiResponse[] classResponses, AnnotatedMethod annotatedMethod) {
        final Operation operation = super.parseMethod(cls, method, globalParameters, methodProduces, classProduces, methodConsumes, classConsumes, classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);
        if(method.getReturnType().getName().equals(Optional.class.getCanonicalName())) {
            final var responses = operation.getResponses();
            for(String key : responses.keySet()) {
                if(key.equalsIgnoreCase("default") || key.equals("200")) {
                    final io.swagger.v3.oas.models.responses.ApiResponse response = responses.get(key);
                    final var contents = response.getContent();
                    if(contents != null) {
                        for(String mediaTypeKey : contents.keySet()) {
                            final MediaType mediaType = contents.get(mediaTypeKey);
                            final var schema = mediaType.getSchema();
                            final var ref = schema.get$ref();
                            if(ref != null) {
                                final Schema allOfSchema = new Schema();
                                allOfSchema.set$ref(ref);
                                schema.set$ref(null);
                                schema.setNullable(true);
                                schema.addAllOfItem(allOfSchema);
                            }
                        }
                    }
                }
            }
        }
        return operation;
    }
}
