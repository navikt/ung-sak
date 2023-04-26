package no.nav.k9.sak.kontrakt.opplæringspenger.vurdering;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_NØDVENDIGHET)
public class VurderNødvendighetDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostIdDto journalpostId;

    @JsonProperty(value = "nødvendigOpplæring", required = true)
    @NotNull
    private boolean nødvendigOpplæring;

    @JsonProperty(value = "tilknyttedeDokumenter")
    @Size(max = 100)
    @Valid
    private Set<String> tilknyttedeDokumenter;

    public VurderNødvendighetDto() {
    }

    public VurderNødvendighetDto(JournalpostIdDto journalpostId, boolean nødvendigOpplæring, String begrunnelse, Set<String> tilknyttedeDokumenter) {
        super(begrunnelse);
        this.journalpostId = journalpostId;
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.tilknyttedeDokumenter = tilknyttedeDokumenter;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }

    public boolean isNødvendigOpplæring() {
        return nødvendigOpplæring;
    }

    public Set<String> getTilknyttedeDokumenter() {
        return tilknyttedeDokumenter;
    }
}
