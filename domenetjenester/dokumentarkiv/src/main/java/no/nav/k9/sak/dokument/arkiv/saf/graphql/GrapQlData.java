package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.*;

import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentoversiktFagsak;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GrapQlData {

    @JsonProperty("journalpost")
    private Journalpost journalpost;

    @JsonProperty("dokumentoversiktFagsak")
    private DokumentoversiktFagsak dokumentoversiktFagsak;

    @JsonCreator
    public GrapQlData(@JsonProperty("journalpost") Journalpost journalpost,
                      @JsonProperty("dokumentoversiktFagsakQuery") DokumentoversiktFagsak dokumentoversiktFagsak) {
        this.journalpost = journalpost;
        this.dokumentoversiktFagsak = dokumentoversiktFagsak;
    }


    public Journalpost getJournalpost() {
        return journalpost;
    }

    public DokumentoversiktFagsak getDokumentoversiktFagsak() {
        return dokumentoversiktFagsak;
    }
}
