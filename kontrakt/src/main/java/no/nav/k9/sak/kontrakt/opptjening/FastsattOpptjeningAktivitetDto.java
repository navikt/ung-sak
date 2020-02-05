package no.nav.k9.sak.kontrakt.opptjening;


import java.time.LocalDate;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class FastsattOpptjeningAktivitetDto {
    private LocalDate fom;
    private LocalDate tom;
    private OpptjeningAktivitetType type;
    private OpptjeningAktivitetKlassifisering klasse;
    private String aktivitetReferanse;
    private String arbeidsgiverNavn;

    public FastsattOpptjeningAktivitetDto() {
        // trengs for deserialisering av JSON
    }

    public FastsattOpptjeningAktivitetDto(LocalDate fom, LocalDate tom, OpptjeningAktivitetKlassifisering klasse) {
        this.fom = fom;
        this.tom = tom;
        this.klasse = klasse;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public OpptjeningAktivitetKlassifisering getKlasse() {
        return klasse;
    }

    public String getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

}
