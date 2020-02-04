package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;

/** Fasade tjeneste eksponert fra modul for å hente opp beregningsgrunnlag i andre moduler. */
@ApplicationScoped
public class HentBeregningsgrunnlagTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public HentBeregningsgrunnlagTjeneste() {
        // for CDI
    }
    
    public HentBeregningsgrunnlagTjeneste(EntityManager entityManager) {
        this(new BeregningsgrunnlagRepository(entityManager));
    }
    
    @Inject
    public HentBeregningsgrunnlagTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }
    
    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Long behandlingId, 
                                                                                                                 Optional<Long> originalBehandlingId,
                                                                                                                 BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, originalBehandlingId, beregningsgrunnlagTilstand);
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentBeregningsgrunnlagGrunnlagEntitet(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentNestSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Long behandlingId, 
                                                                                                                     Optional<Long> originalBehandlingId,
                                                                                                                     BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return beregningsgrunnlagRepository.hentNestSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, originalBehandlingId, beregningsgrunnlagTilstand);
    }

    public BeregningsgrunnlagEntitet hentBeregningsgrunnlagAggregatForBehandling(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
    }

    public Optional<BeregningsgrunnlagGrunnlagEntitet> hentSisteBeregningsgrunnlagGrunnlagEntitet(Long behandlingid, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingid, beregningsgrunnlagTilstand);
    }

    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagForId(Long beregningsgrunnlagId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForId(beregningsgrunnlagId);
    }

    public Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlagForBehandling(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingId);
    }
}
