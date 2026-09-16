package no.nav.ung.ytelse.aktivitetspenger.historikkinnslag;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;

public record HistorikkinnslagData(
    Utfall utfall,
    Avslagsårsak avslagsårsak
) {

    public HistorikkinnslagData(Utfall utfall, IkkeOppfyltDetaljertÅrsak detaljertÅrsak) {
        this(utfall, detaljertÅrsak != null ? detaljertÅrsak.avslagsårsak().orElseThrow(() -> new IllegalArgumentException("Kan ikke ha avslag uten avslagsårsak. Fantes ikke for " + detaljertÅrsak)) : null);
    }

    static HistorikkinnslagData oppfylt() {
        return new HistorikkinnslagData(Utfall.OPPFYLT, (Avslagsårsak) null);
    }

    static HistorikkinnslagData avslått(Avslagsårsak avslagsårsak) {
        return new HistorikkinnslagData(Utfall.IKKE_OPPFYLT, avslagsårsak);
    }

}
