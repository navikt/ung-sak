package no.nav.ung.sak.formidling.vedtak.resultat;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface DetaljertResultatTidslinjeUtleder {

    static DetaljertResultatTidslinjeUtleder finnTjeneste(Instance<DetaljertResultatTidslinjeUtleder> instanser, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instanser, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Fant ingen DetaljertResultatTidslinjeUtleder for fagsakYtelseType " + fagsakYtelseType));
    }

    LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling);
}
