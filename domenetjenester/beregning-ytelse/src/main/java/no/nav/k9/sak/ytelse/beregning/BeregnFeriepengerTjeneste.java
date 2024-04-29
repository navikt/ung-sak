package no.nav.k9.sak.ytelse.beregning;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;

public interface BeregnFeriepengerTjeneste {

    static BeregnFeriepengerTjeneste finnTjeneste(Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjenester, fagsakYtelseType).orElseThrow(() -> new IllegalArgumentException("Har ikke BeregnFeriepengerTjeneste for " + fagsakYtelseType));
    }

    void beregnFeriepenger(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);

    FeriepengeOppsummering beregnFeriepengerOppsummering(BehandlingReferanse ref, BeregningsresultatEntitet beregningsresultat);

}
