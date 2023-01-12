package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkDetaljertUtfall {

    @JsonProperty(value = "gjelderKravstiller")
    @Valid
    private StønadstatistikkKravstillerType kravstiller;

    @JsonProperty(value = "gjelderAktivitetType")
    @Valid
    private String type;

    @JsonProperty(value = "gjelderOrganisasjonsnummer")
    @Valid
    private String organisasjonsnummer;

    @JsonProperty(value = "gjelderAktørId")
    @Valid
    private String aktørId;

    @JsonProperty(value = "gjelderArbeidsforholdId")
    @Valid
    private String arbeidsforholdId;

    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private StønadstatistikkUtfall utfall;

    @AssertTrue
    boolean isMinstEnSpesifiserendeIdentifikatorSatt() {
        return kravstiller != null || type != null || organisasjonsnummer != null || aktørId != null || arbeidsforholdId != null;
    }

    public StønadstatistikkDetaljertUtfall() {
    }

    public StønadstatistikkDetaljertUtfall(StønadstatistikkKravstillerType kravstiller, String type, String organisasjonsnummer, String aktørId, String arbeidsforholdId, StønadstatistikkUtfall utfall) {
        this.kravstiller = kravstiller;
        this.type = type;
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.arbeidsforholdId = arbeidsforholdId;
        this.utfall = utfall;
    }

    public StønadstatistikkKravstillerType getKravstiller() {
        return kravstiller;
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

    public StønadstatistikkUtfall getUtfall() {
        return utfall;
    }

    @Override
    public String toString() {
        return "StønadstatistikkDetaljertUtfall{" +
            (type != null ? "type='" + type + '\'' : "") +
            (organisasjonsnummer != null ? ", organisasjonsnummer='" + organisasjonsnummer + '\'' : "") +
            (aktørId != null ? ", aktørId='" + aktørId + '\'' : "") +
            (arbeidsforholdId != null ? ", arbeidsforholdId='" + arbeidsforholdId + '\'' : "") +
            ", utfall=" + utfall +
            '}';
    }
}
