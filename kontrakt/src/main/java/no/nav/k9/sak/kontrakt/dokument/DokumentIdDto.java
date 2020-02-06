package no.nav.k9.sak.kontrakt.dokument;

import javax.validation.constraints.Digits;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;

public class DokumentIdDto {
    @Digits(integer = 18, fraction = 0)
    private String dokumentId;

    public DokumentIdDto(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    @AbacAttributt("dokumentId")
    public String getDokumentId() {
        return dokumentId;
    }

}
