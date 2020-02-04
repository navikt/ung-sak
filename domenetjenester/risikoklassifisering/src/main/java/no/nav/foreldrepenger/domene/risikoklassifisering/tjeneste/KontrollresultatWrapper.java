package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import java.util.Objects;
import java.util.UUID;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;

public class KontrollresultatWrapper {

    private UUID behandlingUuid;

    private Kontrollresultat kontrollresultatkode;

    public KontrollresultatWrapper(UUID behandlingUuid, Kontrollresultat kontrollresultatkode) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");
        Objects.requireNonNull(kontrollresultatkode, "kontrollresultatKode");
        this.behandlingUuid = behandlingUuid;
        this.kontrollresultatkode = kontrollresultatkode;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Kontrollresultat getKontrollresultatkode() {
        return kontrollresultatkode;
    }
}
