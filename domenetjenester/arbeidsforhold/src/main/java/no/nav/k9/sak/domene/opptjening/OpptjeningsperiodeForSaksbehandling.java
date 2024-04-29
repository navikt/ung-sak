package no.nav.k9.sak.domene.opptjening;

import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

public class OpptjeningsperiodeForSaksbehandling {

    private OpptjeningAktivitetType opptjeningAktivitetType;
    private Opptjeningsnøkkel grupperingNøkkel;
    private Arbeidsgiver arbeidsgiver;
    private Stillingsprosent stillingsprosent;
    private DatoIntervallEntitet periode;
    private VurderingsStatus vurderingsStatus;
    private String begrunnelse;
    private String arbeidsgiverUtlandNavn;

    public OpptjeningsperiodeForSaksbehandling() {
    }
    public OpptjeningsperiodeForSaksbehandling(OpptjeningsperiodeForSaksbehandling fra) {
        this.opptjeningAktivitetType = fra.getOpptjeningAktivitetType();
        this.grupperingNøkkel = fra.grupperingNøkkel;
        this.arbeidsgiver = fra.getArbeidsgiver();
        this.stillingsprosent = fra.getStillingsprosent();
        this.periode = fra.getPeriode();
        this.vurderingsStatus = fra.getVurderingsStatus();
        this.begrunnelse = fra.getBegrunnelse();
        this.arbeidsgiverUtlandNavn = fra.getArbeidsgiverUtlandNavn();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public Opptjeningsnøkkel getOpptjeningsnøkkel() {
        return grupperingNøkkel;
    }

    /**
     * Bruk {@link #getArbeidsgiver} i stedet.
     */
    @Deprecated(forRemoval = true)
    public String getOrgnr() {
        return arbeidsgiver == null ? null : arbeidsgiver.getOrgnr();
    }

    public Stillingsprosent getStillingsprosent() {
        return stillingsprosent;
    }

    public VurderingsStatus getVurderingsStatus() {
        return vurderingsStatus;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverUtlandNavn() {
        return arbeidsgiverUtlandNavn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpptjeningsperiodeForSaksbehandling other = (OpptjeningsperiodeForSaksbehandling) o;
        return Objects.equals(opptjeningAktivitetType, other.opptjeningAktivitetType) &&
            Objects.equals(grupperingNøkkel, other.grupperingNøkkel) &&
            Objects.equals(begrunnelse, other.begrunnelse);
    }

    @Override
    public int hashCode() {

        return Objects.hash(opptjeningAktivitetType, grupperingNøkkel, begrunnelse);
    }

    @Override
    public String toString() {
        return "OpptjeningsperiodeForSaksbehandling{" +
            "opptjeningAktivitetType=" + opptjeningAktivitetType +
            ", grupperingNøkkel=" + grupperingNøkkel +
            ", arbeidsgiver=" + arbeidsgiver +
            ", stillingsprosent=" + stillingsprosent +
            ", periode=" + periode +
            ", vurderingsStatus=" + vurderingsStatus +
            ", begrunnelse='" + begrunnelse + '\'' +
            ", arbeidsgiverUtlandNavn='" + arbeidsgiverUtlandNavn + '\'' +
            '}';
    }

    public static class Builder {
        protected OpptjeningsperiodeForSaksbehandling kladd;

        protected Builder() {
        }

        private Builder(OpptjeningsperiodeForSaksbehandling periode) {
            kladd = periode;
        }

        public static Builder kopi(OpptjeningsperiodeForSaksbehandling periode) {
            return new Builder(new OpptjeningsperiodeForSaksbehandling(periode));
        }

        public static Builder ny() {
            return new Builder(new OpptjeningsperiodeForSaksbehandling());
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType type) {
            Objects.requireNonNull(type, "opptjeningAktivitetType");
            kladd.opptjeningAktivitetType = type;
            return this;
        }

        public Builder medOpptjeningsnøkkel(Opptjeningsnøkkel opptjeningsnøkkel) {
            kladd.grupperingNøkkel = opptjeningsnøkkel;
            return this;
        }

        public Builder medStillingsandel(Stillingsprosent stillingsprosent) {
            kladd.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medVurderingsStatus(VurderingsStatus status) {
            kladd.vurderingsStatus = status;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medArbeidsgiverUtlandNavn(String arbeidsgiverNavn) {
            kladd.arbeidsgiverUtlandNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public OpptjeningsperiodeForSaksbehandling build() {
            valider();
            return kladd;
        }

        protected void valider() {
            // Opptjeningsperiode av typen arbeid krever alltid arbeidsgiver
            if (kladd.opptjeningAktivitetType == OpptjeningAktivitetType.ARBEID && kladd.arbeidsgiver == null
                && (kladd.grupperingNøkkel == null || kladd.grupperingNøkkel.getArbeidsgiverType() == null)) {
                throw new IllegalStateException("Informasjon om arbeidsgiver mangler for " + toString());
            }
        }

    }


}
