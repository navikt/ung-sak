package no.nav.ung.sak.kontrakt;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Representerer en link til en resource/action i en HATEOAS response.
 *
 * @see https://restfulapi.net/hateoas/
 * @see https://tools.ietf.org/html/rfc5988
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ResourceLink {

    @JsonProperty(value = "href", required = true)
    @NotNull
    @Valid
    private URI href;
    /**
     * Link relationship type.
     */
    @JsonProperty(value = "rel", required = true)
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String rel;
    @JsonProperty(value = "requestPayload")
    @Valid
    private Object requestPayload;
    /**
     * Http Method type.
     */
    @JsonProperty(value = "type", required = true)
    @NotNull
    private HttpMethod type;
    @JsonIgnore
    private boolean eksternAdresse = false;

    /**
     * Ctor lager default GET link.
     */
    public ResourceLink(String href, String rel) {
        this(href, rel, HttpMethod.GET);
    }

    public ResourceLink(String href, String rel, HttpMethod type) {
        try {
            this.href = new URI(href);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.rel = rel;
        this.type = type;
    }

    public ResourceLink(String href, String rel, HttpMethod type, Object requestPayload) {
        this(href, rel, type);
        this.requestPayload = requestPayload;
    }

    public ResourceLink(String href, String rel, HttpMethod type, boolean eksternAdresse, Object requestPayload) {
        this(href, rel, type);
        this.eksternAdresse = eksternAdresse;
        this.requestPayload = requestPayload;
    }

    public ResourceLink(URI href, String rel, HttpMethod type, boolean eksternAdresse) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.eksternAdresse = eksternAdresse;
    }

    @SuppressWarnings("unused")
    private ResourceLink() { // NOSONAR
        this((URI) null, null, null, false); // for Jackson
    }

    public static ResourceLink get(String href, String rel) {
        return new ResourceLink(href, rel, HttpMethod.GET, null);
    }

    public static ResourceLink get(String href, String rel, Object queryParams) {
        StringBuilder uri = new StringBuilder();
        uri.append(href);
        if (queryParams != null) {
            uri.append("?");
            uri.append(RestUtils.convertObjectToQueryString(queryParams));
        }
        return new ResourceLink(uri.toString(), rel, HttpMethod.GET, null);
    }

    public static ResourceLink eksternGet(String href, String rel, Object queryParams) {
        StringBuilder uri = new StringBuilder();
        uri.append(href);
        if (queryParams != null) {
            uri.append("?");
            uri.append(RestUtils.convertObjectToQueryString(queryParams));
        }
        return new ResourceLink(uri.toString(), rel, HttpMethod.GET, true, null);
    }

    public static ResourceLink getFraMap(String href, String rel, Map<String, String> queryParams) {
        StringBuilder uri = new StringBuilder();
        uri.append(href);
        if (queryParams != null) {
            uri.append("?");
            uri.append(RestUtils.convertObjectToQueryStringFraMap(queryParams));
        }
        return new ResourceLink(uri.toString(), rel, HttpMethod.GET, false, null);
    }

    public static ResourceLink post(String href, String rel, Object requestPayload) {
        return new ResourceLink(href, rel, HttpMethod.POST, false, requestPayload);
    }

    public static ResourceLink eksternPost(String href, String rel, Object requestPayload) {
        return new ResourceLink(href, rel, HttpMethod.POST, true, requestPayload);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !this.getClass().equals(obj.getClass())) {
            return false;
        }
        ResourceLink other = (ResourceLink) obj;
        return Objects.equals(this.href, other.href)
            && Objects.equals(this.rel, other.rel)
            && Objects.equals(this.type, other.type);
    }

    public URI getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public Object getRequestPayload() {
        return requestPayload;
    }

    public boolean getErEksternAdresse() {
        return eksternAdresse;
    }

    public HttpMethod getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, rel, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + type + " " + href + " [" + rel + "]>";
    }

    public enum HttpMethod {
        DELETE,
        GET,
        PATCH,
        POST,
        PUT,
    }

}
