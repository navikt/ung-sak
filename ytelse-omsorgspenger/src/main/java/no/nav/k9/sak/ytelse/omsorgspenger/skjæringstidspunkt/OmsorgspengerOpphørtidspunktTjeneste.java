package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerOpphørtidspunktTjeneste {

    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerOpphørtidspunktTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OmsorgspengerOpphørtidspunktTjeneste(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        if (erOpphør(ref)) {
            var årskvantumResultat = hentÅrskvantumResultat(ref);
            return årskvantumResultat == null ? Optional.empty() : Optional.ofNullable(getMaksPeriode(årskvantumResultat)).map(Periode::getTom);
        }
        return Optional.empty();
    }

    public boolean harAvslåttPeriode(BehandlingReferanse ref) {
        var resultat = hentÅrskvantumResultat(ref);
        if (resultat == null) {
            return false;
        } else {
            for (Aktivitet uttaksPlanOmsorgspengerAktivitet : resultat.getSisteUttaksplan().getAktiviteter()) {
                for (Uttaksperiode uttaksperiodeOmsorgspenger : uttaksPlanOmsorgspengerAktivitet.getUttaksperioder()) {
                    if (Utfall.AVSLÅTT.equals(uttaksperiodeOmsorgspenger.getUtfall())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean erOpphør(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }

    private ÅrskvantumForbrukteDager hentÅrskvantumResultat(BehandlingReferanse ref) {
        var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
        return årskvantumResultat;
    }


    private Periode getMaksPeriode(ÅrskvantumForbrukteDager årskvantumResultat) {
        var fom = årskvantumResultat.getSisteUttaksplan().getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(Uttaksperiode::getPeriode).map(LukketPeriode::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        var tom = årskvantumResultat.getSisteUttaksplan().getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(Uttaksperiode::getPeriode).map(LukketPeriode::getTom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        return fom != null && tom != null ? new Periode(fom, tom) : null;
    }

}
