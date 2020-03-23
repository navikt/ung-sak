package no.nav.k9.sak.domene.uttak;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.typer.Saksnummer;

public interface UttakTjeneste {

    boolean harAvslåttUttakPeriode(UUID behandlingId);

    Map<UUID, Uttaksplan> hentUttaksplaner(UUID... behandlingId);

    Optional<Uttaksplan> hentUttaksplan(UUID behandlingId);

    Uttaksplan opprettUttaksplan(UttakInput input);

    Map<Saksnummer, Uttaksplan> hentUttaksplaner(List<Saksnummer> saksnummere);

    String hentUttaksplanerRaw(UUID behandlingId);

    String hentUttaksplanerRaw(List<Saksnummer> saksnummere);

}