package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Uttaksplan;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@Dependent
public class OmsorgspengerOpphørtidspunktTjeneste {

    private ÅrskvantumTjeneste årskvantumTjeneste;
    private boolean rammevedtakSammenstillingIÅrskvantum;

    @Inject
    public OmsorgspengerOpphørtidspunktTjeneste(ÅrskvantumTjeneste årskvantumTjeneste,
                                                @KonfigVerdi(value = "OMP_RAMMEVEDTAK_SAMMENSTILLNG_AARSKVANTUM", defaultVerdi = "false") boolean rammevedtakSammenstillingIÅrskvantum) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.rammevedtakSammenstillingIÅrskvantum = rammevedtakSammenstillingIÅrskvantum;
    }

    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        if (erOpphør(ref)) {
            //TODO undersøk om dette er død kode, ser ikke at behandlinger får OPPHØR satt som resultat
            if (rammevedtakSammenstillingIÅrskvantum){
                var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumForBehandlingV2(ref.getBehandlingUuid());
                return årskvantumResultat == null ? Optional.empty() : Optional.ofNullable(getMaksPeriode(årskvantumResultat.getSisteUttaksplan())).map(Periode::getTom);
            }
            var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
            return årskvantumResultat == null ? Optional.empty() : Optional.ofNullable(getMaksPeriode(årskvantumResultat.getSisteUttaksplan())).map(Periode::getTom);
        }
        return Optional.empty();
    }

    private boolean erOpphør(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }


    private Periode getMaksPeriode(Uttaksplan uttaksplan) {
        var fom = uttaksplan.getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(Uttaksperiode::getPeriode).map(LukketPeriode::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        var tom = uttaksplan.getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(Uttaksperiode::getPeriode).map(LukketPeriode::getTom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        return fom != null && tom != null ? new Periode(fom, tom) : null;
    }

}
