package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

// Aktivitetsnøkkel er en gjengivelse av hvordan BeregningsresultatAndel identifiserer en unik godkjent aktivitet
// Er først og fremst knyttet til arbeidsforhold.
// Kompenserer for at det ikke finnes noen slik abstraksjon tilgjengelig i Beregningsresultat sin kodebase
class Aktivitetsnøkkel {
    private final Arbeidsgiver arbeidsgiver;
    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final AktivitetStatus aktivitetStatus;
    private final Inntektskategori inntektskategori;

    Aktivitetsnøkkel(BeregningsresultatAndel andel) {
        this.arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        this.arbeidsforholdRef = andel.getArbeidsforholdRef();
        this.aktivitetStatus = andel.getAktivitetStatus();
        this.inntektskategori = andel.getInntektskategori();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aktivitetsnøkkel)){
            return false;
        }
        Aktivitetsnøkkel that = (Aktivitetsnøkkel) o;

        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef)
            && Objects.equals(aktivitetStatus, that.aktivitetStatus)
            && Objects.equals(inntektskategori, that.inntektskategori)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef, aktivitetStatus, inntektskategori);
    }
}
