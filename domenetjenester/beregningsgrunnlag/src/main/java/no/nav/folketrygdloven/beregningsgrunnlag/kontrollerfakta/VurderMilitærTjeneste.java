package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@ApplicationScoped
public class VurderMilitærTjeneste {

    private OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperiodeTjeneste;

    private final OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();

    VurderMilitærTjeneste() {
        // For CDI
    }

    @Inject
    public VurderMilitærTjeneste(OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste) {
        this.opptjeningsperiodeTjeneste = opptjeningsperioderTjeneste;
    }

    public boolean harOppgittMilitærIOpptjeningsperioden(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag) {

        List<OpptjeningsperiodeForSaksbehandling> opptjeningRelevantForBeregning = opptjeningsperiodeTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening);

        return opptjeningRelevantForBeregning.stream()
            .anyMatch(opptjening -> opptjening.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE));
    }
}
