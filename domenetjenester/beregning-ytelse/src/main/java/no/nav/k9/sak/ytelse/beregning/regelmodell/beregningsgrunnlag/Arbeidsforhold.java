package no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.util.Objects;

import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class Arbeidsforhold {
    private String orgnr;
    private String aktørId;
    private ReferanseType referanseType;
    private InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nullRef();
    private boolean frilanser;

    Arbeidsforhold() {
    }

    public String getIdentifikator() {
        if (aktørId != null) {
            return aktørId;
        }
        return orgnr;
    }

    public InternArbeidsforholdRef getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public boolean erFrilanser() {
        return frilanser;
    }

    public ReferanseType getReferanseType() {
        return referanseType;
    }

    @Override
    public boolean equals(Object annet) {
        if (!(annet instanceof Arbeidsforhold)) {
            return false;
        }
        Arbeidsforhold annetAF = (Arbeidsforhold)annet;
        return Objects.equals(frilanser, annetAF.frilanser)
                && Objects.equals(orgnr, annetAF.orgnr)
                && Objects.equals(aktørId, annetAF.aktørId)
                && Objects.equals(referanseType, annetAF.referanseType)
                && Objects.equals(arbeidsforholdId, annetAF.arbeidsforholdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frilanser, orgnr, arbeidsforholdId, aktørId, referanseType);
    }

    @Override
    public String toString() {
        return "<Arbeidsforhold "
                + "orgnr=" + orgnr + ", "
                + "referanseType=" + referanseType + ", "
                + "arbeidsforholdId=" + arbeidsforholdId + ", "
                + "frilanser=" + frilanser
                + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Arbeidsforhold arbeidsforhold;

        private Builder() {
            arbeidsforhold = new Arbeidsforhold();
        }

        public Builder medOrgnr(String orgnr) {
            verifiserReferanseType();
            arbeidsforhold.orgnr = orgnr;
            arbeidsforhold.referanseType = ReferanseType.ORG_NR;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            verifiserReferanseType();
            arbeidsforhold.aktørId = aktørId;
            arbeidsforhold.referanseType = ReferanseType.AKTØR_ID;
            return this;
        }

        public Builder medArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
            Objects.requireNonNull(arbeidsforholdId, "Bruk InternArbeidsforholdRef#nullRef hvis null");
            arbeidsforhold.arbeidsforholdId = arbeidsforholdId;
            return this;
        }

        public Builder medFrilanser(boolean frilanser) {
            arbeidsforhold.frilanser = frilanser;
            return this;
        }

        public Arbeidsforhold build() {
            verifyForBuild();
            return arbeidsforhold;
        }

        private void verifyForBuild() {
            if (arbeidsforhold.frilanser) {
                arbeidsforhold.orgnr = null;
                arbeidsforhold.arbeidsforholdId = null;
            } else {
                Objects.requireNonNull(arbeidsforhold.referanseType);
                Objects.requireNonNull(arbeidsforhold.arbeidsforholdId, "Bruk InternArbeidsforholdRef#nullRef hvis null");
            }
            if (arbeidsforhold.referanseType == ReferanseType.ORG_NR) {
                Objects.requireNonNull(arbeidsforhold.orgnr, "orgnummer");
            } else if (arbeidsforhold.referanseType == ReferanseType.AKTØR_ID) {
                Objects.requireNonNull(arbeidsforhold.aktørId, "aktør id");
            }
        }

        private void verifiserReferanseType() {
            if (arbeidsforhold.referanseType != null) {
                throw new IllegalStateException("Referansetype er allerede satt på arbeidsforholdet: " + arbeidsforhold.referanseType);
            }
        }

    }

    public static Arbeidsforhold frilansArbeidsforhold() {
        return Arbeidsforhold.builder().medFrilanser(true).build();
    }

    public static Arbeidsforhold nyttArbeidsforholdHosVirksomhet(String orgnr) {
        return Arbeidsforhold.builder().medOrgnr(orgnr).medArbeidsforholdId(InternArbeidsforholdRef.nullRef()).build();
    }

    public static Arbeidsforhold nyttArbeidsforholdHosVirksomhet(String orgnr, InternArbeidsforholdRef arbeidsforholdId) {
        Objects.requireNonNull(arbeidsforholdId, "Bruk InternArbeidsforholdRef#nullRef hvis null");
        return Arbeidsforhold.builder().medOrgnr(orgnr).medArbeidsforholdId(arbeidsforholdId).build();
    }

    public static Arbeidsforhold nyttArbeidsforholdHosPrivatperson(String aktørId, InternArbeidsforholdRef arbeidsforholdId) {
        Objects.requireNonNull(arbeidsforholdId, "Bruk InternArbeidsforholdRef#nullRef hvis null");
        return Arbeidsforhold.builder().medAktørId(aktørId).medArbeidsforholdId(arbeidsforholdId).build();
    }

}
