package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
    @Valid
    private OrgNummer orgnr;

    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "arbeidsforholdId")
    @Valid
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverstyrUttakArbeidsforholdDto that = (OverstyrUttakArbeidsforholdDto) o;
        return type == that.type && Objects.equals(orgnr, that.orgnr) && Objects.equals(aktørId, that.aktørId) && Objects.equals(internArbeidsforholdId, that.internArbeidsforholdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, orgnr, aktørId, internArbeidsforholdId);
    }
}
