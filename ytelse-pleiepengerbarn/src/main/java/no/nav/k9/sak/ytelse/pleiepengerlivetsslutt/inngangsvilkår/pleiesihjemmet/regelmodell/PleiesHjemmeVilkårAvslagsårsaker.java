package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.pleiesihjemmet.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum PleiesHjemmeVilkårAvslagsårsaker {
    PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME("1080", "Pleietrengende innlagt i stedet for hjemme");

    private final String kode;
    private final String årsak;

    PleiesHjemmeVilkårAvslagsårsaker(String kode, String årsak) {
        this.kode = kode;
        this.årsak = årsak;
    }

    public String getKode() {
        return kode;
    }


    RuleReasonRef toRuleReason() {
        return new RuleReasonRefImpl(kode, årsak);
    }
}
