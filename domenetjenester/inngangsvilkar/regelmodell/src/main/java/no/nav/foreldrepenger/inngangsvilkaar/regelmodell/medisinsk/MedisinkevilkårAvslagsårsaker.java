package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum MedisinkevilkårAvslagsårsaker {
    IKKE_BEHOV_FOR_PLEIE("1067", "Ikke tilstrekkelig medisinsk behov.");

    private final String kode;
    private final String årsak;

    MedisinkevilkårAvslagsårsaker(String kode, String årsak) {
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
