package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public record AktivitetsIdentifikator(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref,
                                      no.nav.k9.kodeverk.arbeidsforhold.ArbeidType arbeidType) {
    @Override
    public InternArbeidsforholdRef ref() {
        return ref == null ? InternArbeidsforholdRef.nullRef() : ref;
    }
}
