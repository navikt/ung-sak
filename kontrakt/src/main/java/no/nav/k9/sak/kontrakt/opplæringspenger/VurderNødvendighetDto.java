package no.nav.k9.sak.kontrakt.opplæringspenger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    public VurderNødvendighetDto() {
    }

    public VurderNødvendighetDto(JournalpostIdDto journalpostId, boolean nødvendigOpplæring) {
        this.journalpostId = journalpostId;
        this.nødvendigOpplæring = nødvendigOpplæring;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }

    public boolean isNødvendigOpplæring() {
        return nødvendigOpplæring;
    }
}
