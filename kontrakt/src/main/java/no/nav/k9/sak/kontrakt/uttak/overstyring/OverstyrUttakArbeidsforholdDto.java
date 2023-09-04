package no.nav.k9.sak.kontrakt.uttak.overstyring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrUttakArbeidsforholdDto {

    @JsonProperty(value = "type", required = true)
    @NotNull
    private UttakArbeidType type;

    @JsonProperty(value = "orgnr")
    private OrgNummer orgnr;

    @JsonProperty(value = "aktørId")
    private AktørId aktørId;

    @JsonProperty(value = "arbeidsforholdId")
    private InternArbeidsforholdRef internArbeidsforholdId;

    public OverstyrUttakArbeidsforholdDto() {
    }

    public OverstyrUttakArbeidsforholdDto(UttakArbeidType type, OrgNummer orgnr, AktørId aktørId, InternArbeidsforholdRef internArbeidsforholdId) {
        this.type = type;
        this.orgnr = orgnr;
        this.aktørId = aktørId;
        this.internArbeidsforholdId = internArbeidsforholdId;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public OrgNummer getOrgnr() {
        return orgnr;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public InternArbeidsforholdRef getInternArbeidsforholdId() {
        return internArbeidsforholdId;
    }
}
