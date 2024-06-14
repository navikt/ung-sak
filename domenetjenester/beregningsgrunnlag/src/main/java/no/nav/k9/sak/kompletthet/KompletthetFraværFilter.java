package no.nav.k9.sak.kompletthet;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface KompletthetFraværFilter {

    static KompletthetFraværFilter finnTjeneste(Instance<KompletthetFraværFilter> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(KompletthetFraværFilter.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType));
    }

    boolean harFraværFraArbeidetIPerioden(BehandlingReferanse ref, DatoIntervallEntitet periode, ManglendeVedlegg manglendeVedlegg);
}
