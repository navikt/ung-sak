package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.skjæringstidspunkt.YtelseOpphørtidspunktTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerOpphørtidspunktTjeneste implements YtelseOpphørtidspunktTjeneste {

    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerOpphørtidspunktTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OmsorgspengerOpphørtidspunktTjeneste(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public boolean erOpphør(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }

    @Override
    public Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref) {
        if (!ref.getBehandlingResultat().isBehandlingsresultatOpphørt()) {
            return null; // ikke relevant //NOSONAR
        }

        // TODO K9 Omsorgspenger: Tore Endestad, trengs dette for omsorgspenger? Isåfall hvilken dato bør det være?
        return null;
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        ÅrskvantumResultat årskvantumResultat = hentÅrskvantumResultat(ref);
        return årskvantumResultat == null ? Optional.empty() : Optional.ofNullable(årskvantumResultat.getMaksPeriode()).map(Periode::getTom);
    }

    private ÅrskvantumResultat hentÅrskvantumResultat(BehandlingReferanse ref) {
        ÅrskvantumResultat årskvantumResultat = årskvantumTjeneste.hentÅrskvantumUttak(ref);
        return årskvantumResultat;
    }

    public boolean harAvslåttPeriode(BehandlingReferanse ref) {
        var resultat = hentÅrskvantumResultat(ref);
        if (resultat == null) {
            return false;
        } else if (resultat.getSamletUtfall() == OmsorgspengerUtfall.AVSLÅTT) {
            return true;
        } else {
            return resultat.getUttaksperioder().stream().anyMatch(u -> u.getUtfall()==OmsorgspengerUtfall.AVSLÅTT);
        }
    }
}
