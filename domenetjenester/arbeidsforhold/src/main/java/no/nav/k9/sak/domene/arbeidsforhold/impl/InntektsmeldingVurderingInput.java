package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Objects;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public class InntektsmeldingVurderingInput {

    private BehandlingReferanse referanse;
    private InntektArbeidYtelseGrunnlag grunnlag;

    public InntektsmeldingVurderingInput(BehandlingReferanse referanse) {
        this.referanse = Objects.requireNonNull(referanse);
    }

    public InntektsmeldingVurderingInput(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlag) {
        this.referanse = referanse;
        this.grunnlag = grunnlag;
    }

    public InntektsmeldingVurderingInput medGrunnlag(InntektArbeidYtelseGrunnlag grunnlag) {
        this.grunnlag = grunnlag;
        return this;
    }

    public BehandlingReferanse getReferanse() {
        return referanse;
    }

    public InntektArbeidYtelseGrunnlag getGrunnlag() {
        return grunnlag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InntektsmeldingVurderingInput that = (InntektsmeldingVurderingInput) o;
        return Objects.equals(referanse, that.referanse) && Objects.equals(grunnlag, that.grunnlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanse, grunnlag);
    }

    @Override
    public String toString() {
        return "InntektsmeldingVurderingInput{" +
            "referanse=" + referanse +
            ", grunnlag=" + grunnlag +
            '}';
    }
}
