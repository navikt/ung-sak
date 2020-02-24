package no.nav.k9.sak.kontrakt.dokument;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.k9.abac.AbacAttributt;

public class DokumentProdusertDto {

    @Valid
    @NotNull
    private UUID behandlingUuid;

    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String dokumentMal;

    public DokumentProdusertDto() {
        // trengs for deserialisering av JSON
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getDokumentMal() {
        return dokumentMal;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public void setDokumentMal(String dokumentMal) {
        this.dokumentMal = dokumentMal;
    }

}
