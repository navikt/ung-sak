package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum OmsorgenForAvslagsårsaker {
    IKKE_DOKUMENTERT_OMSORGEN_FOR("1071", "Ikke dokumentert omsorgen for.");

    private final String kode;
    private final String årsak;

    OmsorgenForAvslagsårsaker(String kode, String årsak) {
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
