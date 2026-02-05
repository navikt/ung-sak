package no.nav.ung.sak.kontrakt.dokument;

import jakarta.validation.constraints.Digits;
import no.nav.ung.sak.abac.AppAbacAttributt;
import no.nav.ung.sak.abac.AppAbacAttributtType;

public class DokumentIdDto {
    @Digits(integer = 18, fraction = 0)
    private String dokumentId;

    public DokumentIdDto(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    @AppAbacAttributt(AppAbacAttributtType.DOKUMENT_ID)
    public String getDokumentId() {
        return dokumentId;
    }

}
