package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse;

import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BarnetilleggDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.BeregningDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning.SatsgrunnlagDto;

public record SatsOgBeregningDto(
    int aldersgrenseSatsendring,
    boolean harLavSatstype,
    BeregningDto beregningsgrunnlag,
    SatsgrunnlagDto minsteYtelsegrunnlag,
    SatsgrunnlagDto minsteYtelsegrunnlagOvergangTilHøySats,
    long grunnbeløp,
    BarnetilleggDto barnetillegg
) {
}
