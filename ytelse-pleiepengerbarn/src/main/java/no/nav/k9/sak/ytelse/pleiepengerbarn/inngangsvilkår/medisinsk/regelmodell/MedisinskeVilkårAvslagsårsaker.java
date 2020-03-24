package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum MedisinskeVilkårAvslagsårsaker {
    IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE("1067", "Ikke dokumentert sykdom, skade eller lyte."),
    DOKUMENTASJON_IKKE_FRA_RETT_ORGAN("1068", "Ikke mottatt dokumentasjon fra rett organ."),
    IKKE_BEHOV_FOR_KONTINUERLIG_PLEIE("1069", "Ikke behov for kontinuerlig pleie.");

    private final String kode;
    private final String årsak;

    MedisinskeVilkårAvslagsårsaker(String kode, String årsak) {
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
