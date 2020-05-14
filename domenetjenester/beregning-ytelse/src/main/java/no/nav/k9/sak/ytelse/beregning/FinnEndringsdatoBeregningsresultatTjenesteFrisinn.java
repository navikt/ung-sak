package no.nav.k9.sak.ytelse.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FinnEndringsdatoBeregningsresultatTjenesteFrisinn implements FinnEndringsdatoBeregningsresultatTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister;

    FinnEndringsdatoBeregningsresultatTjenesteFrisinn() {
        // NOSONAR
    }

    @Inject
    public FinnEndringsdatoBeregningsresultatTjenesteFrisinn(BeregningsresultatRepository beregningsresultatRepository,
                                                             FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.finnEndringsdatoMellomPeriodeLister = finnEndringsdatoMellomPeriodeLister;
    }

    @Override
    public Optional<LocalDate> finnEndringsdato(Behandling behandling, BeregningsresultatEntitet revurderingBeregningsresultat) {
        if (behandling.erRevurdering()) {
            return finnEndringsdatoForRevurdering(behandling, revurderingBeregningsresultat);
        } else {
            throw FinnEndringsdatoFeil.FACTORY.behandlingErIkkeEnRevurdering(behandling.getId()).toException();
        }
    }

    private Optional<LocalDate> finnEndringsdatoForRevurdering(Behandling revurdering, BeregningsresultatEntitet revurderingBeregningsresultat) {
        Behandling originalBehandling = revurdering.getOriginalBehandling()
            .orElseThrow(() -> FinnEndringsdatoFeil.FACTORY.manglendeOriginalBehandling(revurdering.getId()).toException());
        Optional<BeregningsresultatEntitet> originalBeregningsresultatFPOpt = beregningsresultatRepository.hentUtbetBeregningsresultat(originalBehandling.getId());
        if (!originalBeregningsresultatFPOpt.isPresent()) {
            return Optional.empty();
        }
        BeregningsresultatEntitet originalBeregningsresultat = originalBeregningsresultatFPOpt.get();
        List<BeregningsresultatPeriode> originalePerioder = originalBeregningsresultat.getBeregningsresultatPerioder();
        if (originalePerioder.isEmpty()) {
            Long id = originalBeregningsresultat.getId();
            throw FinnEndringsdatoFeil.FACTORY.manglendeBeregningsresultatPeriode(id).toException();
        }
        List<BeregningsresultatPeriode> revurderingPerioder = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        if (revurderingPerioder.isEmpty()) {
            Long id = revurderingBeregningsresultat.getId();
            throw FinnEndringsdatoFeil.FACTORY.manglendeBeregningsresultatPeriode(id).toException();
        }
        return finnEndringsdatoMellomPeriodeLister.finnEndringsdato(revurderingPerioder, originalePerioder);
    }

}
