package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

enum AldersvilkårBarnKanIkkeVurdereAutomatiskÅrsaker {
    KAN_IKKE_AUTOMATISK_INNVILGE_ALDERSVILKÅR_BARN(AksjonspunktKodeDefinisjon.VURDER_ALDERSVILKÅR_BARN, "Kan ikke automatisk innvilge aldersvilkåret for barn.");

    private final String kode;
    private final String årsak;

    AldersvilkårBarnKanIkkeVurdereAutomatiskÅrsaker(String kode, String årsak) {
        this.kode = kode;
        this.årsak = årsak;
    }

    public String getKode() {
        return kode;
    }

    public String getÅrsak() {
        return årsak;
    }

    RuleReasonRef toRuleReason() {
        return new RuleReasonRefImpl(kode, årsak);
    }
}
