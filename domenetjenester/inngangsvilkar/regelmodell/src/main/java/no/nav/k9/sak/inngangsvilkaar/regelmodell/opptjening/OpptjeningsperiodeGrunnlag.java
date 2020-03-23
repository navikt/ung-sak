package no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.time.Period;

import no.nav.k9.sak.inngangsvilkaar.regelmodell.VilkårGrunnlag;

public class OpptjeningsperiodeGrunnlag implements VilkårGrunnlag {

    // Input til regel
    private LocalDate førsteUttaksDato;
    private LocalDate hendelsesDato;

    private Period periodeLengde;

    // Settes i løpet av regelevaluering

    private LocalDate skjæringsdatoOpptjening;
    private LocalDate opptjeningsperiodeFom;
    private LocalDate opptjeningsperiodeTom;
    public OpptjeningsperiodeGrunnlag() {
    }

    public OpptjeningsperiodeGrunnlag(LocalDate førsteUttaksDato, LocalDate hendelsesDato) {
        this.førsteUttaksDato = førsteUttaksDato;
        this.hendelsesDato = hendelsesDato;
    }

    public LocalDate getFørsteUttaksDato() {
        return førsteUttaksDato;
    }

    public LocalDate getHendelsesDato() {
        return hendelsesDato;
    }

    public Period getPeriodeLengde() { return periodeLengde; }

    public void setFørsteUttaksDato(LocalDate førsteUttaksDato) {
        this.førsteUttaksDato = førsteUttaksDato;
    }

    public void setHendelsesDato(LocalDate hendelsesDato) {
        this.hendelsesDato = hendelsesDato;
    }

    public void setPeriodeLengde(Period periodeLengde) {
        this.periodeLengde = periodeLengde;
    }

    public LocalDate getSkjæringsdatoOpptjening() {
        return skjæringsdatoOpptjening;
    }

    public LocalDate getOpptjeningsperiodeFom() {
        return opptjeningsperiodeFom;
    }

    public LocalDate getOpptjeningsperiodeTom() {
        return opptjeningsperiodeTom;
    }

    public void setSkjæringsdatoOpptjening(LocalDate skjæringsdatoOpptjening) {
        this.skjæringsdatoOpptjening = skjæringsdatoOpptjening;
    }

    public void setOpptjeningsperiodeFom(LocalDate opptjeningsperiodeFom) {
        this.opptjeningsperiodeFom = opptjeningsperiodeFom;
    }

    public void setOpptjeningsperiodeTom(LocalDate opptjeningsperiodeTom) {
        this.opptjeningsperiodeTom = opptjeningsperiodeTom;
    }

}


