package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

enum OmsorgenForKanIkkeVurdereAutomatiskÅrsaker {
    KAN_IKKE_AUTOMATISK_INNVILGE_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE, "Kan ikke automatisk innvilge omsorgen for.");

    private final String kode;
    private final String årsak;

    OmsorgenForKanIkkeVurdereAutomatiskÅrsaker(String kode, String årsak) {
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
