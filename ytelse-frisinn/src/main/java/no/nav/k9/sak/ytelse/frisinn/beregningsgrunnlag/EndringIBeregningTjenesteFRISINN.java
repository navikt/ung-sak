package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.Utbetalingsendring.UGUNST;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class EndringIBeregningTjenesteFRISINN implements ErEndringIBeregningVurderer {

    private UttakRepository uttakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    EndringIBeregningTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public EndringIBeregningTjenesteFRISINN(UttakRepository uttakRepository,
                                            BeregningsresultatRepository beregningsresultatRepository) {
        this.uttakRepository = uttakRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    /**
     * Frisinn vurderer om behandlingens {@link BeregningsresultatEntitet} er endret, og ikke {@link Beregningsgrunnlag}
     * Dette fordi kun siste del av beregningsgrunnlaget brukes i beregningsresultatet ved ny søknadsperiode. Resten
     * av beregningsresultatet kopieres fra forrige beregningsresultat.
     *
     * @param orginalbehandling revurderingens orginalbehandling
     * @param revurdering gjeldende behandling
     * @param skjæringstidspunkt skjæringstidspunkt for Frisinn
     * @return revurdering er til ugunst (redusert tilkjent ytelse)
     */
    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalbehandling, BehandlingReferanse revurdering, LocalDate skjæringstidspunkt) {
        UttakAktivitet orginaltUttak = uttakRepository.hentFastsattUttak(orginalbehandling.getBehandlingId());

        Optional<BeregningsresultatEntitet> orginaltResultat = beregningsresultatRepository.hentBeregningsresultat(orginalbehandling.getId());
        Optional<BeregningsresultatEntitet> revurderingResultat = beregningsresultatRepository.hentBeregningsresultat(revurdering.getId());

        return ErEndringIBeregningsresultatFRISINN.finnEndringerIUtbetalinger(revurderingResultat, orginaltResultat, orginaltUttak)
            .stream()
            .anyMatch(endring -> endring.equals(UGUNST));
    }

}
