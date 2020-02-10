package no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.Tid;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Opplysningsperiode {

    @JsonAlias("fom")
    @JsonProperty(value = "fraOgMed")
    private LocalDate fraOgMed;

    @JsonAlias("tom")
    @JsonProperty(value = "tilOgMed", required = true)
    @NotNull
    private LocalDate tilOgMed;

    public Opplysningsperiode() {
        fraOgMed = null;
        tilOgMed = Tid.TIDENES_ENDE;
    }

    public Opplysningsperiode(LocalDate fraOgMed, LocalDate tilOgMed) {
        this.fraOgMed = fraOgMed;
        if (tilOgMed != null) {
            this.tilOgMed = tilOgMed;
        } else {
            this.tilOgMed = Tid.TIDENES_ENDE;
        }
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }
}
