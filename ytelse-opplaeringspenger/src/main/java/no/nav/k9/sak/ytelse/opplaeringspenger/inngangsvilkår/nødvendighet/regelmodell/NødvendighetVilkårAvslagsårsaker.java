package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

enum NødvendighetVilkårAvslagsårsaker {
    IKKE_NØDVENDIG(Avslagsårsak.IKKE_NØDVENDIG.getKode(), Avslagsårsak.IKKE_NØDVENDIG.getNavn());

    private final String kode;
    private final String årsak;

    NødvendighetVilkårAvslagsårsaker(String kode, String årsak) {
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
