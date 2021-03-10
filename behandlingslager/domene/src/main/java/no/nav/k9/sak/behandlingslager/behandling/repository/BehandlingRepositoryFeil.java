package no.nav.k9.sak.behandlingslager.behandling.repository;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface BehandlingRepositoryFeil extends DeklarerteFeil {
    BehandlingRepositoryFeil FACTORY = FeilFactory.create(BehandlingRepositoryFeil.class);

    @TekniskFeil(feilkode = "FP-131239", feilmelding = "Fant ikke entitet for låsing [%s], id=%s.", logLevel = LogLevel.ERROR)
    Feil fantIkkeEntitetForLåsing(String entityClassName, long id);

    @TekniskFeil(feilkode = "FP-131240", feilmelding = "Fant ikke BehandlingVedtak, behandlingId=%s.", logLevel = LogLevel.ERROR)
    Feil fantIkkeBehandlingVedtak(Object behandlingId);

}
