package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Objects;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class BeregningsresultatAktivitetsnøkkel {
    private final Arbeidsgiver arbeidsgiver;
    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final AktivitetStatus aktivitetStatus;
    private final Inntektskategori inntektskategori;

    BeregningsresultatAktivitetsnøkkel(BeregningsresultatAndel andel) {
        this.arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        this.arbeidsforholdRef = andel.getArbeidsforholdRef();
        this.aktivitetStatus = andel.getAktivitetStatus();
        this.inntektskategori = andel.getInntektskategori();
    }

    @Override
    public String toString() {
        return "BeregningsresultatAktivitetsnøkkel{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", aktivitetStatus=" + aktivitetStatus +
            ", inntektskategori=" + inntektskategori +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningsresultatAktivitetsnøkkel)){
            return false;
        }
        BeregningsresultatAktivitetsnøkkel that = (BeregningsresultatAktivitetsnøkkel) o;

        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef)
            && Objects.equals(aktivitetStatus, that.aktivitetStatus)
            && Objects.equals(inntektskategori, that.inntektskategori);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef, aktivitetStatus, inntektskategori);
    }
}
