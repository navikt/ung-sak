package no.nav.ung.sak.økonomi.simulering.klient;

import no.nav.k9.oppdrag.kontrakt.Domene;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public class DomeneUtil {

    private DomeneUtil() {
    }

    public static Domene ytelsestypeTilAbacDomene(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case UNGDOMSYTELSE -> Domene.UNG;
            case AKTIVITETSPENGER -> Domene.AKTIVITETSPENGER;
            default -> throw new IllegalStateException("Ikke-støttet ytelsestype: " + ytelseType);
        };

    }

}


