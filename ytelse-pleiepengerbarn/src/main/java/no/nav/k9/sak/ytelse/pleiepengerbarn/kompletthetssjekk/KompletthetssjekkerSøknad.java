package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@Dependent
public class KompletthetssjekkerSøknad {

    private Period ventefristForTidligSøknad;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    KompletthetssjekkerSøknad() {
        // for proxy
    }

    @Inject
    public KompletthetssjekkerSøknad(SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                     @KonfigVerdi(value = "fp.ventefrist.tidlig.soeknad", defaultVerdi = "P4W") Period ventefristForTidligSøknad) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.ventefristForTidligSøknad = ventefristForTidligSøknad;
    }

    public Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDate> permisjonsstart = søknadsperiodeTjeneste.utledFullstendigPeriode(ref.getBehandlingId()).stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo);
        if (permisjonsstart.isPresent()) {
            LocalDate ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            boolean erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                LocalDateTime ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }
}
