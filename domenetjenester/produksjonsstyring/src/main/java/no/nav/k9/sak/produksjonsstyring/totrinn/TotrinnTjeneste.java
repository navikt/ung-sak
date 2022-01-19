package no.nav.k9.sak.produksjonsstyring.totrinn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

/** Totrinn tjeneste som eksponeres ut av modulen.*/
@ApplicationScoped
public class TotrinnTjeneste {

    private TotrinnRepository totrinnRepository;

    TotrinnTjeneste() {
        // CDI
    }

    @Inject
    public TotrinnTjeneste(TotrinnRepository totrinnRepository) {
        this.totrinnRepository = totrinnRepository;
    }

    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlagHvisEksisterer(Behandling behandling) {
        return totrinnRepository.hentTotrinngrunnlag(behandling);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Behandling behandling) {
        return totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
    }

    public void settNyeTotrinnaksjonspunktvurderinger(Behandling behandling, List<Totrinnsvurdering> vurderinger) {
        totrinnRepository.lagreOgFlush(behandling, vurderinger);
    }
    
    public void lagreNyttTotrinnresultat(Behandling behandling, Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        totrinnRepository.lagreOgFlush(behandling, totrinnresultatgrunnlag);
    }
}
