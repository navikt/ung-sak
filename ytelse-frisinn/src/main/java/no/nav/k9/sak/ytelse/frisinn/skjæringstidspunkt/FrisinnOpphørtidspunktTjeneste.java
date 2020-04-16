package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.YtelseOpphørtidspunktTjeneste;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnOpphørtidspunktTjeneste implements YtelseOpphørtidspunktTjeneste {

    private UttakTjeneste uttakTjeneste;
    private OpphørUttakTjeneste opphørUttakTjeneste;

    protected FrisinnOpphørtidspunktTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FrisinnOpphørtidspunktTjeneste(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
        this.opphørUttakTjeneste = new OpphørUttakTjeneste(uttakTjeneste);
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

        var uttakOpt = uttakTjeneste.hentUttaksplan(ref.getBehandlingUuid());
        return uttakOpt.map(uttaksplan -> uttaksplan.harInnvilgetPerioder()).orElse(false);
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return opphørUttakTjeneste.getOpphørsdato(ref);
    }
    
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        return opphørUttakTjeneste.harAvslåttUttakPeriode(behandlingUuid);
    }
}
