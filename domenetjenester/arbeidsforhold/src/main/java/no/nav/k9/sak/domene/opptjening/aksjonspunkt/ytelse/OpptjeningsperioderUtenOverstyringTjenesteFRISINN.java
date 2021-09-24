package no.nav.k9.sak.domene.opptjening.aksjonspunkt.ytelse;

import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.sak.domene.opptjening.ytelse.OpptjeningsperiodeForSaksbehandlingFRISINN;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

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
    protected OpptjeningsperiodeForSaksbehandling mapOppgittArbeidsperiode(OppgittArbeidsforhold oppgittArbeidsforhold, OpptjeningAktivitetType type, VurderingsStatus status) {
        final OpptjeningsperiodeForSaksbehandlingFRISINN.Builder builder = new OpptjeningsperiodeForSaksbehandlingFRISINN.Builder();
        DatoIntervallEntitet periode = oppgittArbeidsforhold.getPeriode();
        builder.medOpptjeningAktivitetType(type)
            .medVurderingsStatus(status)
            .medPeriode(periode);
        return builder.build();
    }

    @Override
    protected DatoIntervallEntitet finnFrilansPeriode(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet periode, Collection<Yrkesaktivitet> frilansOppdrag) {
        return periode;
    }

}
