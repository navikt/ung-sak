package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.EndringIBeregningTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;

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
        UttakAktivitet uttakAktivitet = uttakRepository.hentFastsattUttak(orginalbehandling.getBehandlingId());
        LocalDate førsteUttaksdato = uttakAktivitet.getPerioder().stream().map(p -> p.getPeriode().getFomDato()).min(LocalDate::compareTo).orElseThrow();
        return ErEndringIBeregningFRISINN.vurder(revurderingsGrunnlag, originaltGrunnlag, førsteUttaksdato);
    }


}
