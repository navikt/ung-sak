package no.nav.k9.sak.domene.medlem.impl;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;

public interface MedlemFeil extends DeklarerteFeil {

    MedlemFeil FACTORY = FeilFactory.create(MedlemFeil.class);

    @IntegrasjonFeil(feilkode = "FP-085790", feilmelding = "Feil ved kall til medlemskap tjenesten.", logLevel = LogLevel.ERROR)
    Feil feilVedKallTilMedlem(Exception e);
}
