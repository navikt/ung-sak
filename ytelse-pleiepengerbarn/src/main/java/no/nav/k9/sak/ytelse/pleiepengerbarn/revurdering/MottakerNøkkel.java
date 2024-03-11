package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public record MottakerNøkkel(Boolean brukerErMottaker,
                             Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef,
                             AktivitetStatus aktivitetStatus, Inntektskategori inntektskategori) {

    @Override
    public InternArbeidsforholdRef arbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

}
