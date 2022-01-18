package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StønadstatistikkArbeidsforhold {

    @JsonProperty(value = "type", required = true)
    @NotNull
    @Valid
    private String type;
    
    @JsonProperty(value = "organisasjonsnummer")
    @Valid
    private String organisasjonsnummer;
    
    @JsonProperty(value = "aktørId")
    @Valid
    private String aktørId;
    
    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    private String arbeidsforholdId;
    
    
    protected StønadstatistikkArbeidsforhold() {
        
    }
    
    public StønadstatistikkArbeidsforhold(String type, String organisasjonsnummer, String aktørId, String arbeidsforholdId) {
        this.type = type;
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.arbeidsforholdId = arbeidsforholdId;
    }
    
    
    public String getType() {
        return type;
    }
    
    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }
    
    public String getAktørId() {
        return aktørId;
    }
    
    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    @Override
    public String toString() {
        return "StønadstatistikkArbeidsforhold [type=" + type + ", organisasjonsnummer=" + organisasjonsnummer
                + ", aktørId=" + aktørId + ", arbeidsforholdId=" + arbeidsforholdId + "]";
    }
}
