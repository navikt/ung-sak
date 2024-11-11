package no.nav.ung.sak.kontrakt.dokument;

import jakarta.validation.constraints.Digits;

import no.nav.k9.abac.AbacAttributt;

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
