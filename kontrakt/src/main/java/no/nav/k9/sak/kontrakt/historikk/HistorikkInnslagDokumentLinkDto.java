package no.nav.k9.sak.kontrakt.historikk;

import java.net.URI;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkInnslagDokumentLinkDto {

    @JsonProperty(value="tag")
    @Size(max = 1000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String tag;
    
    @JsonProperty(value="url")
    private URI url;

    @JsonProperty(value="journalpostId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String journalpostId;

    @JsonProperty(value="dokumentId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String dokumentId;
    
    @JsonProperty(value="utgått")
    private boolean utgått;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = URI.create(url);
    }

    public boolean isUtgått() {
        return utgått;
    }

    public void setUtgått(boolean utgått) {
        this.utgått = utgått;
    }
}
