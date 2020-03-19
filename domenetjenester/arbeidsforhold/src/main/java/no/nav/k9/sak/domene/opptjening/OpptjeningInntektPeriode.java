package no.nav.k9.sak.domene.opptjening;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;

public class OpptjeningInntektPeriode {

    private LocalDate fraOgMed;
    private LocalDate tilOgMed;
    private BigDecimal beløp;
    private Opptjeningsnøkkel opptjeningsnøkkel;
    private InntektspostType type;

    public OpptjeningInntektPeriode(Inntektspost inntektspost, Opptjeningsnøkkel opptjeningsnøkkel) {
        this.fraOgMed = inntektspost.getPeriode().getFomDato();
        this.tilOgMed = inntektspost.getPeriode().getTomDato();
        this.beløp = inntektspost.getBeløp().getVerdi();
        this.opptjeningsnøkkel = opptjeningsnøkkel;
        this.type = inntektspost.getInntektspostType();
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    public Opptjeningsnøkkel getOpptjeningsnøkkel() {
        return opptjeningsnøkkel;
    }

    public InntektspostType getType() {
        return type;
    }
}
