package no.nav.ung.sak.ytelse.beregning;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;

public interface BeregnFeriepengerTjeneste {

    static BeregnFeriepengerTjeneste finnTjeneste(Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjenester, fagsakYtelseType).orElseThrow(() -> new IllegalArgumentException("Har ikke BeregnFeriepengerTjeneste for " + fagsakYtelseType));
    }

    void beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);


}
