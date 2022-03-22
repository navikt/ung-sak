package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

enum AleneOmOmsorgenKanIkkeVurdereAutomatiskÅrsaker {
    KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.VURDER_OMS_UTVIDET_RETT, "Kan ikke automatisk innvilge vilkåret alene om omsorgen.");

    private final String kode;
    private final String årsak;

    AleneOmOmsorgenKanIkkeVurdereAutomatiskÅrsaker(String kode, String årsak) {
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
