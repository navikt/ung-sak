package no.nav.k9.sak.hendelse.stønadstatistikk.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StønadstatistikkArbeidsforhold {

    @JsonProperty(value = "type", required = true)
    @NotNull
    @Valid
    private String type;
    
    @JsonProperty(value = "organisasjonsnummer", required = true)
    @NotNull
    @Valid
    private String organisasjonsnummer;
    
    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    private String aktørId;
    
    @JsonProperty(value = "arbeidsforholdId", required = true)
    @NotNull
    @Valid
    private String arbeidsforholdId;
    
    
    public StønadstatistikkArbeidsforhold() {
        
    }
    
    public StønadstatistikkArbeidsforhold(String type, String organisasjonsnummer, String aktørId, String arbeidsforholdId) {
        this.type = type;
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.arbeidsforholdId = arbeidsforholdId;
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
