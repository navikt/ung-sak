package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.Optional;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.typer.AktørId;

public interface InfotrygdMigreringTjeneste {

    static Optional<InfotrygdMigreringTjeneste> finnTjeneste(Instance<InfotrygdMigreringTjeneste> instanser, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instanser, ytelseType);
    }

    void finnOgOpprettMigrertePerioder(Long behandlingId, AktørId aktørId, Long fagsakId);

}
