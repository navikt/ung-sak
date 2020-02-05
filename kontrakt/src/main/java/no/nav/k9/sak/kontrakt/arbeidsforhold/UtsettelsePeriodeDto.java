package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.UtsettelseÅrsak;
import no.nav.k9.sak.typer.Periode;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility=Visibility.ANY)
public class UtsettelsePeriodeDto {
    
    @JsonProperty("fom")
    private LocalDate fom;
    
    @JsonProperty("tom")
    private LocalDate tom;
    
    @JsonProperty("utsettelseArsak")
    private UtsettelseÅrsak utsettelseÅrsak;

    public UtsettelsePeriodeDto() {
        //
    }
    
    public UtsettelsePeriodeDto(Periode periode, UtsettelseÅrsak utsettelseÅrsak) {
        this.fom = periode.getFom();
        this.tom = periode.getTom();
        this.utsettelseÅrsak = utsettelseÅrsak;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public UtsettelseÅrsak getUtsettelseArsak() {
        return utsettelseÅrsak;
    }
}
