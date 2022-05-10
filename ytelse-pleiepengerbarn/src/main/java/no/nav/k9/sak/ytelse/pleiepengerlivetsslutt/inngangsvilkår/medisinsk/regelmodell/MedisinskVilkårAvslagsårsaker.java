package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum MedisinskVilkårAvslagsårsaker {
    IKKE_I_LIVETS_SLUTTFASE("1081", "Ikke i livets sluttfase"),
    PLEIETRENGENDE_INNLAGT_I_STEDET_FOR_HJEMME("1080", "Pleietrengende innlagt i stedet for hjemme");

    private final String kode;
    private final String årsak;

    MedisinskVilkårAvslagsårsaker(String kode, String årsak) {
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
