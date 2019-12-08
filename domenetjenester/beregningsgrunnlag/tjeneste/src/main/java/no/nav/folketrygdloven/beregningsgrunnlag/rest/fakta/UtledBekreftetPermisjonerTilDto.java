package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.PermisjonDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.HentBekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class UtledBekreftetPermisjonerTilDto {

    private UtledBekreftetPermisjonerTilDto() {
        // skjul default
    }

    static Optional<PermisjonDto> utled(InntektArbeidYtelseGrunnlag grunnlag, LocalDate stp, BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        Arbeidsgiver arbeidsgiver = bgAndelArbeidsforhold.getArbeidsgiver();
        InternArbeidsforholdRef arbeidsforholdRef = bgAndelArbeidsforhold.getArbeidsforholdRef();
        Optional<BekreftetPermisjon> permisjonForYrkesaktivitet = HentBekreftetPermisjon.hent(grunnlag, arbeidsgiver, arbeidsforholdRef);
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = finnBekreftetPermisjonSomOverlapperStp(stp, permisjonForYrkesaktivitet);
        if (bekreftetPermisjonOpt.isPresent()) {
            PermisjonDto dto = lagPermisjonDto(bekreftetPermisjonOpt.get());
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    private static Optional<BekreftetPermisjon> finnBekreftetPermisjonSomOverlapperStp(LocalDate stp, Optional<BekreftetPermisjon> permisjonForYrkesaktivitet) {
        return permisjonForYrkesaktivitet
            .filter(perm -> perm.getStatus().equals(BekreftetPermisjonStatus.BRUK_PERMISJON))
            .filter(perm -> perm.getPeriode().inkluderer(stp));
    }

    private static PermisjonDto lagPermisjonDto(BekreftetPermisjon bekreftetPermisjonOpt) {
        LocalDate fomDato = bekreftetPermisjonOpt.getPeriode().getFomDato();
        LocalDate tomDato = bekreftetPermisjonOpt.getPeriode().getTomDato();
        return new PermisjonDto(fomDato, tomDato);
    }

}
