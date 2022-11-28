package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.nare.RuleService;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface OmsorgenForVilkår extends RuleService<OmsorgenForVilkårGrunnlag> {

    static OmsorgenForVilkår finnTjeneste(Instance<OmsorgenForVilkår> tjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke " + OmsorgenForVilkår.class.getSimpleName() + " for ytelse=" + fagsakYtelseType));
    }
}
