package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrInputForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class BeregningInputOppdaterer implements AksjonspunktOppdaterer<OverstyrInputForBeregningDto> {


    private BeregningPerioderGrunnlagRepository grunnlagRepository;


    BeregningInputOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningInputOppdaterer(BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(OverstyrInputForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        lagreInputOverstyringer(param.getBehandlingId(), dto);
        return OppdateringResultat.utenOverhopp();

    }

    private void lagreInputOverstyringer(Long behandlingId, OverstyrInputForBeregningDto dto) {
        var overstyrtePerioder = dto.getPerioder().stream()
            .map(it -> new InputOverstyringPeriode(it.getPeriode().getFom(), mapAktiviteter(it.getAktivitetliste())))
            .collect(Collectors.toList());
        grunnlagRepository.lagreInputOverstyringer(behandlingId, overstyrtePerioder);
    }

    private List<InputAktivitetOverstyring> mapAktiviteter(List<OverstyrBeregningAktivitet> aktivitetliste) {
        return aktivitetliste.stream()
            .map(a -> new InputAktivitetOverstyring(
                mapArbeidsgiver(a),
                mapBeløp(a.getInntektPrAar()),
                mapBeløp(a.getRefusjonPrAar()),
                a.getAktivitetStatus(),
                mapPeriode(a.getAktivitetsperiode())))
            .collect(Collectors.toList());
    }

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        return periode.getTom() == null ? DatoIntervallEntitet.fraOgMed(periode.getFom()) : DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }

    private Beløp mapBeløp(Integer beløp) {
        return beløp != null ? new Beløp(beløp) : null;
    }

    private Arbeidsgiver mapArbeidsgiver(OverstyrBeregningAktivitet a) {
        if (a.getArbeidsgiverOrgnr() == null && a.getArbeidsgiverAktørId() == null) {
            return null;
        }
        return a.getArbeidsgiverOrgnr() != null ? Arbeidsgiver.virksomhet(a.getArbeidsgiverOrgnr()) : Arbeidsgiver.person(a.getArbeidsgiverAktørId());
    }


}
