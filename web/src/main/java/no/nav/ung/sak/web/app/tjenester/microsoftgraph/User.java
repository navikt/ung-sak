package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record User(@NotNull UUID id,
                   @NotNull String onPremisesSamAccountName,
                   @NotNull String displayName) {
}
