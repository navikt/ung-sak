package no.nav.ung.sak.domene.person.pdl;

import no.nav.k9.felles.integrasjon.pdl.Behandlingsnummer;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.List;

public class BehandlingsnummerMapper {
    private BehandlingsnummerMapper() {
    }

    public static List<Behandlingsnummer> ytelsestypeTilBehandlingsnummer(FagsakYtelseType ytelseType){
        Boolean aktivert = Boolean.valueOf(Environment.current().getProperty("BRUK_PDL_SPESIFIKKE_BEHANDLINGNUMRE", "false"));
        if (!aktivert){
            return List.of(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
        }
        return switch (ytelseType){
            case AKTIVITETSPENGER -> List.of(Behandlingsnummer.AKTIVITETSPENGER);
            case UNGDOMSYTELSE -> List.of(Behandlingsnummer.UNGDOMSPROGRAMYTELSEN);
            default -> throw new IllegalArgumentException("Ikke-støttet ytelsestype: " + ytelseType);
        };
    }
}
