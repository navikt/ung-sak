package no.nav.ung.sak.formidling.domene;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.dto.PartResponseDto;

public record GenerertBrev(
    /*
     * PDF dokumentet
     */
    byte[] pdf,
    /*
     * Mottaker av brevet
     */
    PartResponseDto mottaker,

    /*
     * Hvem brevet gjelder. Kan være en annen enn mottaker
     */
    PartResponseDto gjelder,

    /*
     * MalType brukt
     */
    DokumentMalType malType

) {
}
