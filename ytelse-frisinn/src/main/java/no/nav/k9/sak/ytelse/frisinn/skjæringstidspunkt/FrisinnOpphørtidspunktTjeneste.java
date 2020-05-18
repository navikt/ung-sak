package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.YtelseOpphørtidspunktTjeneste;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnOpphørtidspunktTjeneste implements YtelseOpphørtidspunktTjeneste {

    private UttakRepository uttakRepository;

    protected FrisinnOpphørtidspunktTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FrisinnOpphørtidspunktTjeneste(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public boolean erOpphør(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }

    @Override
    public Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref) {
        if (!erOpphørtRevurdering(ref)) {
            return null; // ikke relevant //NOSONAR
        }
        Optional<LocalDate> førsteUttaksdato = hentFørsteUttaksdag(ref);
        return førsteUttaksdato.isPresent();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        if (!erOpphørtRevurdering(ref)) {
            return Optional.empty(); // ikke relevant
        }
        return hentFørsteUttaksdag(ref);
    }

    private boolean erOpphørtRevurdering(BehandlingReferanse ref) {
        return ref.erRevurdering() && ref.getOriginalBehandlingId().isPresent() && ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }

    private Optional<LocalDate> hentFørsteUttaksdag(BehandlingReferanse ref) {
        UttakAktivitet originaltFastsattUttak = uttakRepository.hentFastsattUttak(ref.getOriginalBehandlingId().get());
        return originaltFastsattUttak.getPerioder().stream()
            .map(p -> p.getPeriode().getFomDato()).min(Comparator.naturalOrder());
    }
}
