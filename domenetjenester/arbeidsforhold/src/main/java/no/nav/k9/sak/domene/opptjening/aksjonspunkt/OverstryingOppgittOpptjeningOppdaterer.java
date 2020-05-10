package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opptjening.BekreftOverstyrOppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.opptjening.OppgittOpptjeningDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftOverstyrOppgittOpptjeningDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstryingOppgittOpptjeningOppdaterer implements AksjonspunktOppdaterer<BekreftOverstyrOppgittOpptjeningDto> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    OverstryingOppgittOpptjeningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstryingOppgittOpptjeningOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftOverstyrOppgittOpptjeningDto dto, AksjonspunktOppdaterParameter param) {

        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();

        OppgittOpptjeningDto oppgittOpptjening = dto.getOppgittOpptjening();

        if (oppgittOpptjening.getOppgittEgenNæring() != null && !oppgittOpptjening.getOppgittEgenNæring().isEmpty()) {
            List<EgenNæringBuilder> egenNæringBuilders = oppgittOpptjening.getOppgittEgenNæring().stream().map(oppgittEgenNæringDto -> {
                EgenNæringBuilder egenNæringBuilder = EgenNæringBuilder.ny();
                egenNæringBuilder.medBruttoInntekt(oppgittEgenNæringDto.getBruttoInntekt().getVerdi());
                egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getTom()));
                return egenNæringBuilder;
            }).collect(Collectors.toList());
            oppgittOpptjeningBuilder.leggTilEgneNæringer(egenNæringBuilders);
        }

        if (oppgittOpptjening.getOppgittFrilans() != null) {
            List<OppgittFrilansoppdrag> collect = oppgittOpptjening.getOppgittFrilans().getOppgittFrilansoppdrag().stream().map(oppgittFrilansoppdragDto -> {
                OppgittFrilansOppdragBuilder frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
                frilansOppdragBuilder.medInntekt(oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi());
                frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
                return frilansOppdragBuilder.build();
            }).collect(Collectors.toList());

            OppgittFrilansBuilder frilansBuilder = OppgittFrilansBuilder.ny();
            frilansBuilder.leggTilOppgittOppdrag(collect);
            oppgittOpptjeningBuilder.leggTilFrilansOpplysninger(frilansBuilder.build());
        }

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(param.getBehandlingId(), oppgittOpptjeningBuilder);

        return OppdateringResultat.utenOveropp();
    }
}
