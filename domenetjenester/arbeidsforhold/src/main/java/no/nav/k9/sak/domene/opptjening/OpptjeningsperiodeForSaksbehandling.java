package no.nav.k9.sak.domene.opptjening;

import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;

public class OpptjeningsperiodeForSaksbehandling {

    protected OpptjeningAktivitetType opptjeningAktivitetType;
    protected Opptjeningsnøkkel grupperingNøkkel;
    protected Arbeidsgiver arbeidsgiver;
    protected Stillingsprosent stillingsprosent;
    protected DatoIntervallEntitet periode;
    protected VurderingsStatus vurderingsStatus;
    protected Boolean erPeriodeEndret = false;
    protected Boolean erManueltRegistrert = false;
    protected String begrunnelse;
    protected Boolean manueltBehandlet = false;
    protected String arbeidsgiverUtlandNavn;

    protected OpptjeningsperiodeForSaksbehandling() {
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

    public Boolean getErManueltRegistrert() {
        return erManueltRegistrert;
    }

    public Boolean getErPeriodeEndret() {
        return erPeriodeEndret;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public boolean erManueltBehandlet() {
        return manueltBehandlet;
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
            Objects.equals(erPeriodeEndret, other.erPeriodeEndret) &&
            Objects.equals(erManueltRegistrert, other.erManueltRegistrert) &&
            Objects.equals(begrunnelse, other.begrunnelse);
    }

    @Override
    public int hashCode() {

        return Objects.hash(opptjeningAktivitetType, grupperingNøkkel, erPeriodeEndret, erManueltRegistrert, begrunnelse);
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
            ", erPeriodeEndret=" + erPeriodeEndret +
            ", erManueltRegistrert=" + erManueltRegistrert +
            ", begrunnelse='" + begrunnelse + '\'' +
            ", manueltBehandlet=" + manueltBehandlet +
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

        public Builder medErManueltRegistrert() {
            kladd.erManueltRegistrert = true;
            return this;
        }

        public Builder medErPeriodenEndret() {
            kladd.erPeriodeEndret = true;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medErManueltBehandlet() {
            kladd.manueltBehandlet = true;
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
