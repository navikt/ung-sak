package no.nav.k9.sak.behandling.hendelse.produksjonsstyring.søknadsårsak;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.UtvidetSøknadÅrsak;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface SøknadsårsakUtleder {
    List<UtvidetSøknadÅrsak> utledSøknadÅrsaker(Behandling behandling);

    static SøknadsårsakUtleder finnTjeneste(Instance<SøknadsårsakUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(SøknadsårsakUtleder.class, instances, ytelseType)
            .orElse(null);
    }
}
