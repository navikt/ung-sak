package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.BekreftOverstyrOppgittOpptjeningDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftOverstyrOppgittOpptjeningDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstyringOppgittOpptjeningOppdaterer implements AksjonspunktOppdaterer<BekreftOverstyrOppgittOpptjeningDto> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;


    OverstyringOppgittOpptjeningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyringOppgittOpptjeningOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftOverstyrOppgittOpptjeningDto dto, AksjonspunktOppdaterParameter param) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(param.getBehandlingId());

        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var oppgittOpptjening = dto.getOppgittOpptjening();

        if (oppgittOpptjening.getOppgittEgenNæring() != null && !oppgittOpptjening.getOppgittEgenNæring().isEmpty()) {
            var egenNæringBuilders = oppgittOpptjening.getOppgittEgenNæring().stream().map(oppgittEgenNæringDto -> {
                var egenNæringBuilder = EgenNæringBuilder.ny();
                egenNæringBuilder.medBruttoInntekt(oppgittEgenNæringDto.getBruttoInntekt().getVerdi());
                egenNæringBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittEgenNæringDto.getPeriode().getFom(), oppgittEgenNæringDto.getPeriode().getTom()));
                return egenNæringBuilder;
            }).collect(Collectors.toList());
            oppgittOpptjeningBuilder.leggTilEgneNæringer(egenNæringBuilders);
        }

        if (oppgittOpptjening.getOppgittFrilans() != null) {
            var collect = oppgittOpptjening.getOppgittFrilans().getOppgittFrilansoppdrag().stream().map(oppgittFrilansoppdragDto -> {
                var frilansOppdragBuilder = OppgittFrilansOppdragBuilder.ny();
                frilansOppdragBuilder.medInntekt(oppgittFrilansoppdragDto.getBruttoInntekt().getVerdi());
                frilansOppdragBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(oppgittFrilansoppdragDto.getPeriode().getFom(), oppgittFrilansoppdragDto.getPeriode().getTom()));
                return frilansOppdragBuilder.build();
            }).collect(Collectors.toList());

            var frilansBuilder = OppgittFrilansBuilder.ny();
            frilansBuilder.leggTilOppgittOppdrag(collect);
            oppgittOpptjeningBuilder.leggTilFrilansOpplysninger(frilansBuilder.build());
        }

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(kontekst.getBehandlingId(), oppgittOpptjeningBuilder);

        return OppdateringResultat.utenOveropp();
    }
}
