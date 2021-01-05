package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDate;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentDto {

    @JsonProperty(value = "id")
    @Valid
    private String id;
    
    @JsonProperty(value = "type")
    @Valid
    private SykdomDokumentType type;
    
    @JsonProperty(value = "benyttet")
    @Valid
    private boolean benyttet;
    
    @JsonProperty(value = "annenPartErKilde")
    @Valid
    private boolean annenPartErKilde;
    
    @JsonProperty(value = "datert")
    @Valid
    private LocalDate datert;
    
    @JsonProperty(value = "fremhevet")
    @Valid
    private boolean fremhevet;

    
    public SykdomDokumentDto(String id, SykdomDokumentType type, boolean benyttet,
            boolean annenPartErKilde, LocalDate datert, boolean fremhevet) {
        this.id = id;
        this.type = type;
        this.benyttet = benyttet;
        this.annenPartErKilde = annenPartErKilde;
        this.datert = datert;
        this.fremhevet = fremhevet;
    }
    
    
}
