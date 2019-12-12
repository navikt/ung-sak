package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import javax.validation.Valid;
import javax.validation.constraints.*;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class NyBehandlingDto implements AbacDto {
    @NotNull
    @Size(
        max = 19
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9]*$"
    )
    private String saksnummer;

    @NotNull
    @ValidKodeverk
    private BehandlingType behandlingType;

    @ValidKodeverk
    private BehandlingÅrsakType behandlingArsakType;

    @Valid
    private boolean nyBehandlingEtterKlage;

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
		this.behandlingType = behandlingType;
	}

    public BehandlingType getBehandlingType() {
		return behandlingType;
	}

    public boolean getNyBehandlingEtterKlage() {
        return nyBehandlingEtterKlage;
    }

    public void setBehandlingArsakType(BehandlingÅrsakType behandlingArsakType) {
		this.behandlingArsakType = behandlingArsakType;
	}

    public BehandlingÅrsakType getBehandlingArsakType() {
		return behandlingArsakType;
	}

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }
}
