package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import static no.nav.k9.kodeverk.behandling.BehandlingType.REVURDERING;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.EndringIUttakPeriodeUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
public class PleiepengerEndringIUttakPeriodeUtleder implements EndringIUttakPeriodeUtleder {


    private UttakTjeneste uttakTjeneste;


    public PleiepengerEndringIUttakPeriodeUtleder() {
    }

    @Inject
    public PleiepengerEndringIUttakPeriodeUtleder(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(BehandlingReferanse behandlingReferanse) {
        if (!behandlingReferanse.getBehandlingType().equals(REVURDERING)) {
            return new TreeSet<>();
        }
        return uttaksendringerSidenForrigeBehandling(behandlingReferanse);
    }

    private NavigableSet<DatoIntervallEntitet> uttaksendringerSidenForrigeBehandling(BehandlingReferanse referanse) {
        final Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(referanse.getBehandlingUuid(), false);
        if (uttaksplan == null) {
            return new TreeSet<>();
        }

        return uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getEndringsstatus() != Endringsstatus.UENDRET)
            .map(entry -> DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().getFom(), entry.getKey().getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
