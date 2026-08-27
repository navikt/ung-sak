package no.nav.ung.ytelse.aktivitetspenger;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.time.LocalDate;

@Dependent
public class AktivitetspengerFagsakperiodeUtleder {

    public DatoIntervallEntitet utledFagsakPeriodeUtvidelse(Behandling behandling, LocalDate søktDato) {
        var originalBehandlingId = behandling.getOriginalBehandlingId();

        LocalDate fagsakTomDato = behandling.getFagsak().getPeriode().getTomDato();
        if (originalBehandlingId.isEmpty() || søktDato.isAfter(fagsakTomDato)) {
            var søknadTidslinje = AktivitetspengerSøknadsperiodeTjeneste.tidslinjeFraSøktDato(søktDato);
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadTidslinje.getMinLocalDate(), søknadTidslinje.getMaxLocalDate());
        }

        return DatoIntervallEntitet.fraOgMedTilOgMed(søktDato, fagsakTomDato);
    }

}
