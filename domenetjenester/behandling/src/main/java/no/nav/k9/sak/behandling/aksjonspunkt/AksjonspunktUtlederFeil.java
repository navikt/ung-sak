package no.nav.k9.sak.behandling.aksjonspunkt;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

import static no.nav.k9.felles.feil.LogLevel.WARN;

public interface AksjonspunktUtlederFeil extends DeklarerteFeil {

    AksjonspunktUtlederFeil FACTORY = FeilFactory.create(AksjonspunktUtlederFeil.class);

    @TekniskFeil(feilkode = "FP-985832", feilmelding = "Ukjent aksjonspunktutleder %s", logLevel = WARN)
    Feil fantIkkeAksjonspunktUtleder(String className);

    @TekniskFeil(feilkode = "FP-191205", feilmelding = "Mer enn en implementasjon funnet for aksjonspunktutleder %s", logLevel = WARN)
    Feil flereImplementasjonerAvAksjonspunktUtleder(String className);
}
