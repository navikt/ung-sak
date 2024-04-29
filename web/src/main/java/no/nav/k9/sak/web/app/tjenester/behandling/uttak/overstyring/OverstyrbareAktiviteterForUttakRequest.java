package no.nav.k9.sak.web.app.tjenester.behandling.uttak.overstyring;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;

public class OverstyrbareAktiviteterForUttakRequest {

    @Valid
    @NotNull
    private BehandlingUuidDto behandlingIdDto;
    @Valid
    @NotNull
    private LocalDate fom;
    @Valid
    @NotNull
    private LocalDate tom;

    public OverstyrbareAktiviteterForUttakRequest(BehandlingUuidDto behandlingIdDto, LocalDate fom, LocalDate tom) {
        this.behandlingIdDto = behandlingIdDto;
        this.fom = fom;
        this.tom = tom;
    }

    public OverstyrbareAktiviteterForUttakRequest() {
    }

    public BehandlingUuidDto getBehandlingIdDto() {
        return behandlingIdDto;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    @AssertTrue(message = "ugyldg periode - fom kan ikke være etter tom")
    boolean gyldigPeriode() {
        return !fom.isAfter(tom);
    }

    @JsonIgnore
    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingIdDto.getBehandlingUuid();
    }
}
