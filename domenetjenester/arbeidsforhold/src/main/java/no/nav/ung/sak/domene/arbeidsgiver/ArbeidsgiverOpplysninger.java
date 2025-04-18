package no.nav.ung.sak.domene.arbeidsgiver;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ArbeidsgiverOpplysninger {

    private final String identifikator;
    private final String alternativIdentifikator;
    private final String navn;
    private LocalDate fødselsdato; // Fødselsdato for privatperson som arbeidsgiver

    public ArbeidsgiverOpplysninger(String identifikator, String alternativIdentifikator, String navn, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.alternativIdentifikator = alternativIdentifikator;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
    }

    public ArbeidsgiverOpplysninger(String identifikator, String navn) {
        this.identifikator = identifikator;
        this.alternativIdentifikator = null;
        this.navn = navn;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public String getAlternativIdentifikator() {
        return alternativIdentifikator;
    }

    public String getIdentifikatorGUI() {
        if (fødselsdato == null) {
            return identifikator;
        } else {
            return fødselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }
}
