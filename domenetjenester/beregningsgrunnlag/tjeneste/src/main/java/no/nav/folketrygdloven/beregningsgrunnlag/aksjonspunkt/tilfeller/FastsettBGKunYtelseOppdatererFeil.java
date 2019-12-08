package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface FastsettBGKunYtelseOppdatererFeil extends DeklarerteFeil {

    FastsettBGKunYtelseOppdatererFeil FACTORY = FeilFactory.create(FastsettBGKunYtelseOppdatererFeil.class); //NOSONAR

    @TekniskFeil(feilkode = "FP-401646", feilmelding = "Finner ikke andelen for eksisterende grunnlag.", logLevel = LogLevel.WARN)
    Feil finnerIkkeAndelFeil();
}
