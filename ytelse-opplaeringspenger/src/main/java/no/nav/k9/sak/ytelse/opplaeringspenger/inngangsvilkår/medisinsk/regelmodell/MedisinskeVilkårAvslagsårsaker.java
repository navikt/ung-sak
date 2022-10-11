package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum MedisinskeVilkårAvslagsårsaker {
    IKKE_LANGVARIG_SYK("1067", "Ikke dokumentert sykdom, skade eller lyte."); // TODO: Endre til noe mer fornuftig

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
