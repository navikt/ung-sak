package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderStatusInput {
    private OpptjeningAktivitetType type;
    private BehandlingReferanse behandlingReferanse;
    private Yrkesaktivitet registerAktivitet;
    private Yrkesaktivitet overstyrtAktivitet;
    private InntektArbeidYtelseGrunnlag iayGrunnlag;
    private boolean harVærtSaksbehandlet;
    private DatoIntervallEntitet opptjeningPeriode;
    private SakInntektsmeldinger inntektsmeldinger;

    public VurderStatusInput(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse) {
        this.type = Objects.requireNonNull(type);
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse);
    }

    public Yrkesaktivitet getRegisterAktivitet() {
        return registerAktivitet;
    }

    public void setRegisterAktivitet(Yrkesaktivitet registerAktivitet) {
        this.registerAktivitet = registerAktivitet;
    }

    public Yrkesaktivitet getOverstyrtAktivitet() {
        return overstyrtAktivitet;
    }

    public void setOverstyrtAktivitet(Yrkesaktivitet overstyrtAktivitet) {
        this.overstyrtAktivitet = overstyrtAktivitet;
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    public void setGrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        this.iayGrunnlag = iayGrunnlag;
    }

    public boolean getHarVærtSaksbehandlet() {
        return harVærtSaksbehandlet;
    }

    public void setHarVærtSaksbehandlet(boolean harVærtSaksbehandlet) {
        this.harVærtSaksbehandlet = harVærtSaksbehandlet;
    }

    public DatoIntervallEntitet getOpptjeningPeriode() {
        return opptjeningPeriode;
    }

    public void setOpptjeningPeriode(DatoIntervallEntitet opptjeningPeriode) {
        this.opptjeningPeriode = opptjeningPeriode;
    }

    public SakInntektsmeldinger getInntektsmeldinger() {
        return inntektsmeldinger;
    }

    public void setInntektsmeldinger(SakInntektsmeldinger inntektsmeldinger) {
        this.inntektsmeldinger = inntektsmeldinger;
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }
}
