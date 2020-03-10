package no.nav.foreldrepenger.domene.uttak;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager).
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (<code>src/test/resources/META-INF/beans.xml</code> for å aktivere for enhetstester) i modul som skal
 * bruke
 * <p>
 * <p>
 */
@RequestScoped
@Alternative
public class UttakInMemoryTjeneste implements UttakTjeneste {

    private final Map<UUID, Uttaksplan> uttaksplaner = new LinkedHashMap<>();

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        if (!uttaksplaner.containsKey(behandlingUuid)) {
            throw new IllegalStateException("Har ikke registrert uttaksplan for behandling: " + behandlingUuid);
        }
        var uttak = uttaksplaner.get(behandlingUuid);
        return uttak.harAvslåttePerioder();
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).stream().findFirst();
    }

    @Override
    public List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        return Arrays.asList(behandlingUuid).stream().map(uuid -> uttaksplaner.get(uuid)).collect(Collectors.toList());
    }

    public void lagreUttakResultatPerioder(UUID behandlingId, Uttaksplan uttaksplan) {
        uttaksplaner.put(behandlingId, uttaksplan);
    }

    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {
        // TODO Auto-generated method stub
        return null;
    }

}
