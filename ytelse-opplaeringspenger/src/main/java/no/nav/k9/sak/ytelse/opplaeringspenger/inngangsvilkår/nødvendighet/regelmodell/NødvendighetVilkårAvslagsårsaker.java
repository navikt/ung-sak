package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;

enum NødvendighetVilkårAvslagsårsaker {
    //TODO hent fra Avlagsårsak (unngå duplisering)
    IKKE_NØDVENDIG("1101", "Ikke nødvendig for omsorgen av pleietrengende"),
    IKKE_GODKJENT_INSTITUSJON("1102", "Institusjonen er ikke godkjent");

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
        return new RuleReasonRefImpl(kode, årsak); //TODO deprecated
    }
}
