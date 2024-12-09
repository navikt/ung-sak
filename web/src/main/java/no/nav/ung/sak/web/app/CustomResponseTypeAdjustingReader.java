package no.nav.ung.sak.web.app;

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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Denne klassen justerer generert openapi spesifikasjon til å stemme med våre behov i to ulike tilfeller.
 * <ul>
 *     <li>
 * Når rest endepunkt returnerer Optional<T> type, altså eit barnetilleggTidslinje som kan vere null eller ein gitt type: Modifiserer
 * generert openapi spesifikasjon for normalrespons slik at respons definisjon blir satt som "nullable": true. For at dette
 * skal fungere må også den opprinnelege respons type $ref flyttast inn i ein "allOf" array.
 *     </li>
 *     <li>
 * Når rest endepunkt returnerer void og det har ikkje blitt generert noko result schema for normal respons: Modifiserer
 * generert openapi spesifikasjon for normalrespons til å ha type "void".
 *     </li>
 * </ul>
 *
 * Disse modifikasjoner skjer altså berre for responser av type "default" eller "200" pr no. Så ein kan med annotasjoner
 * ha andre respons koder som returnerer konkrete andre typer, feks ved feil.
 */
public class CustomResponseTypeAdjustingReader extends Reader {

    private static boolean isNormalResponseKey(final String key) {
        return key.equalsIgnoreCase("default") || key.equalsIgnoreCase("200");
    }

    @Override
    protected Operation parseMethod(Class<?> cls, Method method, List<Parameter> globalParameters, Produces methodProduces, Produces classProduces, Consumes methodConsumes, Consumes classConsumes, List<SecurityRequirement> classSecurityRequirements, Optional<ExternalDocumentation> classExternalDocs, Set<String> classTags, List<Server> classServers, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, JsonView jsonViewAnnotation, ApiResponse[] classResponses, AnnotatedMethod annotatedMethod) {
        final Operation operation = super.parseMethod(cls, method, globalParameters, methodProduces, classProduces, methodConsumes, classConsumes, classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);
        if(method.getReturnType().getName().equals(Optional.class.getCanonicalName())) {
            for(Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                if(isNormalResponseKey(responseEntry.getKey())) {
                    final var contents = responseEntry.getValue().getContent();
                    if(contents != null) {
                        for (Map.Entry<String, MediaType> mediaTypeEntry : contents.entrySet()){
                            if (mediaTypeEntry.getKey().contains("application/json")) {
                                final var schema = mediaTypeEntry.getValue().getSchema();
                                if (schema != null) {
                                    final var ref = schema.get$ref();
                                    if (ref != null) {
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
            }
        }
        if(method.getReturnType().getName().equals("void")) {
            for(Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                if(isNormalResponseKey(responseEntry.getKey())) {
                    final var contents = responseEntry.getValue().getContent();
                    if(contents != null) {
                        for (Map.Entry<String, MediaType> mediaTypeEntry : contents.entrySet()){
                            if (mediaTypeEntry.getKey().contains("application/json")) {
                                if (mediaTypeEntry.getValue().getSchema() == null) {
                                    final Schema schema = new Schema();
                                    schema.setType("void");
                                    mediaTypeEntry.getValue().setSchema(schema);
                                }
                            }
                        }
                    }
                }
            }
        }
        return operation;
    }
}
