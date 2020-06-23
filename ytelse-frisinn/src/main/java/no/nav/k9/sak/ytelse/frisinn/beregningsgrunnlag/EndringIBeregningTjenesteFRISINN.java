package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.DefaultErEndringIBeregningTjeneste;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class EndringIBeregningTjenesteFRISINN implements ErEndringIBeregningVurderer {

    private BeregningTjeneste kalkulusTjeneste;
    private UttakRepository uttakRepository;
    private Boolean skalVurdereMedFeiltoleranse;

    EndringIBeregningTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public EndringIBeregningTjenesteFRISINN(BeregningTjeneste kalkulusTjeneste,
                                            UttakRepository uttakRepository,
                                            @KonfigVerdi(value = "KAN_HA_UGUNST_OPPTIL_RETTSGEBYR", defaultVerdi = "false") Boolean ugunstMedFeiltoleranse) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.uttakRepository = uttakRepository;
        this.skalVurdereMedFeiltoleranse = ugunstMedFeiltoleranse;
    }

    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalbehandling, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        var originaltGrunnlag = kalkulusTjeneste.hentFastsatt(orginalbehandling, skjæringstidspuntk);
        var revurderingsGrunnlag = kalkulusTjeneste.hentFastsatt(revurdering, skjæringstidspuntk);
        UttakAktivitet orginaltUttak = uttakRepository.hentFastsattUttak(orginalbehandling.getBehandlingId());
        if (skalVurdereMedFeiltoleranse) {
            return ErEndringIBeregningRettsgebyrFRISINN.erUgunst(revurderingsGrunnlag, originaltGrunnlag, orginaltUttak);
        } else {
            return ErEndringIBeregningFRISINN.erUgunst(revurderingsGrunnlag, originaltGrunnlag, orginaltUttak);
        }
    }

}
