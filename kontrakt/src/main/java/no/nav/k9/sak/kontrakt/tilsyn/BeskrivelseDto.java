package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.typer.Periode;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeskrivelseDto {


    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "tekst")
    @Size(max = 4000)
    @Valid
    private String tekst;

    @JsonProperty(value = "mottattDato")
    @Valid
    private LocalDate mottattDato;

    @JsonProperty(value = "kilde")
    @Valid
    private Kilde kilde;

    public BeskrivelseDto(Periode periode, String tekst, LocalDate mottattDato, Kilde kilde) {
        this.periode = periode;
        this.tekst = tekst;
        this.mottattDato = mottattDato;
        this.kilde = kilde;

    }

    public Periode getPeriode() {
        return periode;
    }

    public String getTekst() {
        return tekst;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public Kilde getKilde() {
        return kilde;
    }
}
