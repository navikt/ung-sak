package no.nav.ung.sak.kontrakt.dokument;

import jakarta.validation.constraints.Digits;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;

public class DokumentIdDto {
    @Digits(integer = 18, fraction = 0)
    private String dokumentId;

    public DokumentIdDto(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.DOKUMENT_DATA_ID)
    public String getDokumentId() {
        return dokumentId;
    }

}
