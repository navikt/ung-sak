package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.Optional;

import jakarta.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface HenleggelsePostopsTjeneste {

    static Optional<HenleggelsePostopsTjeneste> finnTjeneste(Instance<HenleggelsePostopsTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(HenleggelsePostopsTjeneste.class, instances, ytelseType);
    }

    void utfør(Behandling behandling);
}
