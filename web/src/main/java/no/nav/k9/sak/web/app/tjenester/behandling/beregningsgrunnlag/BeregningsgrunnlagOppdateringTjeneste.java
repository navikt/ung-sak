package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

@ApplicationScoped
public class BeregningsgrunnlagOppdateringTjeneste {

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;

    BeregningsgrunnlagOppdateringTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagOppdateringTjeneste(BeregningTjeneste kalkulusTjeneste,
                                                 BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(Map<LocalDate, HåndterBeregningDto> stpTilDtoMap, BehandlingReferanse ref) {
        // Sjekker at vi ikke oppaterer grunnlag som ikke er til vurdering
        validerOppdatering(stpTilDtoMap, ref);

        return kalkulusTjeneste.oppdaterBeregningListe(stpTilDtoMap, ref);
    }

    private void validerOppdatering(Map<LocalDate, HåndterBeregningDto> stpTilDtoMap,
                                    BehandlingReferanse ref) {
        NavigableSet<DatoIntervallEntitet> perioderSomSkalKunneVurderes = vilkårTjeneste.utledPerioderTilVurdering(ref, false);
        stpTilDtoMap.keySet().forEach(stp -> {
            List<DatoIntervallEntitet> vurderingsperioderSomInkludererSTP = finnPerioderSomInkludererDato(perioderSomSkalKunneVurderes, stp);
            if (vurderingsperioderSomInkludererSTP.size() == 0) {
                throw new IllegalStateException("Prøver å endre grunnlag med skjæringstidspunkt" + stp + " men denne er ikke i" +
                    " listen over vilkårsperioder som er til vurdering " + perioderSomSkalKunneVurderes);
            } else if (vurderingsperioderSomInkludererSTP.size() >= 2) {
                throw new IllegalStateException("Prøver å endre grunnlag med skjæringstidspunkt" + stp + " som finnes i flere perioder som er til vurdering," +
                    " ugyldig tilstand. Perioder som er til vurdering er: " + perioderSomSkalKunneVurderes);
            }
        });
    }

    private List<DatoIntervallEntitet> finnPerioderSomInkludererDato(NavigableSet<DatoIntervallEntitet> perioderSomSkalKunneVurderes, LocalDate stp) {
        return perioderSomSkalKunneVurderes.stream()
            .filter(periode -> periode.inkluderer(stp))
            .collect(Collectors.toList());
    }
}
