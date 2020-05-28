package no.nav.k9.sak.domene.opptjening.ytelse;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

import java.util.Objects;

public class OpptjeningsperiodeForSaksbehandlingFRISINN extends OpptjeningsperiodeForSaksbehandling {

    public OpptjeningsperiodeForSaksbehandlingFRISINN() {
    }

    public static class Builder {
        protected OpptjeningsperiodeForSaksbehandlingFRISINN kladd;

        protected Builder() {
        }

        private Builder(OpptjeningsperiodeForSaksbehandlingFRISINN periode) {
            kladd = periode;
        }

        public static Builder ny() {
            return new Builder(new OpptjeningsperiodeForSaksbehandlingFRISINN());
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medPeriode(DatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medOpptjeningAktivitetType(OpptjeningAktivitetType type) {
            Objects.requireNonNull(type, "opptjeningAktivitetType");
            kladd.opptjeningAktivitetType = type;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medOpptjeningsnøkkel(Opptjeningsnøkkel opptjeningsnøkkel) {
            kladd.grupperingNøkkel = opptjeningsnøkkel;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medStillingsandel(Stillingsprosent stillingsprosent) {
            kladd.stillingsprosent = stillingsprosent;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medVurderingsStatus(VurderingsStatus status) {
            kladd.vurderingsStatus = status;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medErManueltRegistrert() {
            kladd.erManueltRegistrert = true;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medErPeriodenEndret() {
            kladd.erPeriodeEndret = true;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medErManueltBehandlet() {
            kladd.manueltBehandlet = true;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medArbeidsgiverUtlandNavn(String arbeidsgiverNavn) {
            kladd.arbeidsgiverUtlandNavn = arbeidsgiverNavn;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN.Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandlingFRISINN build() {
            return kladd;
        }
    }

}
