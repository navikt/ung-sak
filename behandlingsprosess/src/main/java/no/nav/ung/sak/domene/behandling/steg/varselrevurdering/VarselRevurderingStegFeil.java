package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface VarselRevurderingStegFeil extends DeklarerteFeil {

    VarselRevurderingStegFeil FACTORY = FeilFactory.create(VarselRevurderingStegFeil.class);

    @TekniskFeil(feilkode = "FP-139371", feilmelding = "Manger behandlingsårsak på revurdering", logLevel = LogLevel.ERROR)
    Feil manglerBehandlingsårsakPåRevurdering();
}
