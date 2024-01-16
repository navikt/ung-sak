package no.nav.k9.sak.domene.iay.modell;

import no.nav.k9.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.LønnsinntektBeskrivelse;
import no.nav.k9.kodeverk.arbeidsforhold.SkatteOgAvgiftsregelType;
import no.nav.k9.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InntektspostBuilder {
    private Inntektspost inntektspost;

    InntektspostBuilder(Inntektspost inntektspost) {
        this.inntektspost = inntektspost;
    }

    public static InntektspostBuilder ny() {
        return new InntektspostBuilder(new Inntektspost());
    }

    public InntektspostBuilder medInntektspostType(InntektspostType inntektspostType) {
        this.inntektspost.setInntektspostType(inntektspostType);
        return this;
    }

    public InntektspostBuilder medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType skatteOgAvgiftsregelType) {
        this.inntektspost.setSkatteOgAvgiftsregelType(skatteOgAvgiftsregelType);
        return this;
    }

    public InntektspostBuilder medLønnsinntektBeskrivelse(LønnsinntektBeskrivelse lønnsinntektBeskrivelse) {
        this.inntektspost.setLønnsinntektBeskrivelse(lønnsinntektBeskrivelse);
        return this;
    }

    public InntektspostBuilder medPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
        this.inntektspost.setPeriode(fraOgMed, tilOgMed);
        return this;
    }

    public InntektspostBuilder medBeløp(BigDecimal verdi) {
        this.inntektspost.setBeløp(new Beløp(verdi));
        return this;
    }

    public InntektspostBuilder medInntektYtelse(InntektYtelseType inntektYtelseType) {
        this.inntektspost.setInntektYtelseType(inntektYtelseType);
        return this;
    }

    public Inntektspost build() {
        if (inntektspost.hasValues()) {
            return inntektspost;
        }
        throw new IllegalStateException();
    }

    public InntektspostBuilder medInntektspostType(String kode) {
       return medInntektspostType(InntektspostType.fraKode(kode));
    }

    public InntektspostBuilder medSkatteOgAvgiftsregelType(String kode) {
        return medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType.fraKode(kode));
    }
}
