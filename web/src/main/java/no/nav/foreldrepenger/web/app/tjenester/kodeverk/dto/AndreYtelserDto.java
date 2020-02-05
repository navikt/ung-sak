package no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto;

import java.time.LocalDate;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;

public class AndreYtelserDto {

    private ArbeidType ytelseType;
    private LocalDate periodeFom;
    private LocalDate periodeTom;

    public ArbeidType getYtelseType() {
        return ytelseType;
    }

    public void setYtelseType(ArbeidType ytelseType) {
        this.ytelseType = ytelseType;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }
}
