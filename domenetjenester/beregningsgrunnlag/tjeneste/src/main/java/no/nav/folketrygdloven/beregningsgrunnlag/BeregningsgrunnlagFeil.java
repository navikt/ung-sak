package no.nav.folketrygdloven.beregningsgrunnlag;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface BeregningsgrunnlagFeil extends DeklarerteFeil {
    BeregningsgrunnlagFeil FEILFACTORY = FeilFactory.create(BeregningsgrunnlagFeil.class); //$NON-NLS-1$

    @TekniskFeil(feilkode = "FP-370602", feilmelding = "Kunne ikke serialisere regelinput for beregningsgrunnlag.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisereRegelinput(JsonProcessingException e);
}
