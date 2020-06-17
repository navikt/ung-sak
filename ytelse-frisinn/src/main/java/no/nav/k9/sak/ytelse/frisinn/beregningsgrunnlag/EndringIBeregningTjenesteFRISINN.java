package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.EndringIBeregningTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class EndringIBeregningTjenesteFRISINN extends EndringIBeregningTjeneste {
    private UttakRepository uttakRepository;

    EndringIBeregningTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public EndringIBeregningTjenesteFRISINN(BeregningTjeneste kalkulusTjeneste,
                                            UttakRepository uttakRepository) {
        super(kalkulusTjeneste);
        this.uttakRepository = uttakRepository;
    }

    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalbehandling, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        var originaltGrunnlag = kalkulusTjeneste.hentFastsatt(orginalbehandling, skjæringstidspuntk);
        var revurderingsGrunnlag = kalkulusTjeneste.hentFastsatt(revurdering, skjæringstidspuntk);
        UttakAktivitet orginaltUttak = uttakRepository.hentFastsattUttak(orginalbehandling.getBehandlingId());
        return ErEndringIBeregningFRISINN.erUgunst(revurderingsGrunnlag, originaltGrunnlag, orginaltUttak);
    }


}
