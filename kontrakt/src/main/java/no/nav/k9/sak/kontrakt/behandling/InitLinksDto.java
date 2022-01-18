package no.nav.k9.sak.kontrakt.behandling;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.ResourceLink;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InitLinksDto {

    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    @JsonProperty(value = "sakLinks")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> sakLinks = new ArrayList<>();

    public InitLinksDto(List<ResourceLink> links,
                        List<ResourceLink> linksSak) {
        this.links = links;
        this.sakLinks = linksSak;
    }

    public List<ResourceLink> getLinks() {
        return links;
    }

    public List<ResourceLink> getSakLinks() {
        return sakLinks;
    }
}
