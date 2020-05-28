package no.nav.k9.sak.domene.opptjening.aksjonspunkt.ytelse;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.sak.domene.opptjening.ytelse.OpptjeningsperiodeForSaksbehandlingFRISINN;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class OpptjeningsperioderUtenOverstyringTjenesteFRISINN extends OpptjeningsperioderUtenOverstyringTjeneste {

    public OpptjeningsperioderUtenOverstyringTjenesteFRISINN() {
        // CDI
    }

    @Inject
    public OpptjeningsperioderUtenOverstyringTjenesteFRISINN(OpptjeningRepository opptjeningRepository) {
        super(opptjeningRepository);
    }

    @Override
    protected OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type) {
        final OpptjeningsperiodeForSaksbehandlingFRISINN.Builder builder = OpptjeningsperiodeForSaksbehandlingFRISINN.Builder.ny();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
            .medPeriode(periode);
        return builder.build();
    }
}
