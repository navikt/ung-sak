package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentOversikt {

    @JsonProperty(value = "dokumenter")
    @Size(max = 1000)
    @Valid
    private List<SykdomDokumentOversiktElement> dokumenter = new ArrayList<>();
    
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();
    
    
    public SykdomDokumentOversikt(List<SykdomDokumentOversiktElement> dokumenter,
            List<ResourceLink> links) {
        this.dokumenter = dokumenter;
        this.links = links;
    }
    
    
    public List<SykdomDokumentOversiktElement> getDokumenter() {
        return dokumenter;
    }
    
    public List<ResourceLink> getLinks() {
        return links;
    }
}
