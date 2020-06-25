package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class EndringIBeregningTjenesteFRISINN implements ErEndringIBeregningVurderer {

    private BeregningTjeneste kalkulusTjeneste;
    private UttakRepository uttakRepository;
    private Boolean ugunstVurderesMedBeregningsresultat;
    private BeregningsresultatRepository beregningsresultatRepository;

    EndringIBeregningTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public EndringIBeregningTjenesteFRISINN(BeregningTjeneste kalkulusTjeneste,
                                            UttakRepository uttakRepository,
                                            @KonfigVerdi(value = "UGUNST_VURDERES_MED_BEREGNINGSRESULTAT", defaultVerdi = "true") Boolean ugunstVurderesMedBeregningsresultat,
                                            BeregningsresultatRepository beregningsresultatRepository) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.uttakRepository = uttakRepository;
        this.ugunstVurderesMedBeregningsresultat = ugunstVurderesMedBeregningsresultat;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalbehandling, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        UttakAktivitet orginaltUttak = uttakRepository.hentFastsattUttak(orginalbehandling.getBehandlingId());

        Optional<Beregningsgrunnlag> originaltGrunnlag = kalkulusTjeneste.hentFastsatt(orginalbehandling, skjæringstidspuntk);
        Optional<Beregningsgrunnlag> revurderingsGrunnlag = kalkulusTjeneste.hentFastsatt(revurdering, skjæringstidspuntk);

        Optional<BeregningsresultatEntitet> orginaltResultat = beregningsresultatRepository.hentBeregningsresultat(orginalbehandling.getId());
        Optional<BeregningsresultatEntitet> revurderingResultat = beregningsresultatRepository.hentBeregningsresultat(revurdering.getId());

        if (ugunstVurderesMedBeregningsresultat) {
            return ErEndringIBeregningsresultatFRISINN.erUgunst(revurderingResultat, orginaltResultat, orginaltUttak);
        } else {
            return ErEndringIBeregningFRISINN.erUgunst(revurderingsGrunnlag, originaltGrunnlag, orginaltUttak);
        }
    }

}
