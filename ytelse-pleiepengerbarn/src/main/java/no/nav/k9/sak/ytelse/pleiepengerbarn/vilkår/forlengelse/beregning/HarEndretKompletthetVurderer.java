package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

@ApplicationScoped
public class HarEndretKompletthetVurderer {

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    @Inject
    public HarEndretKompletthetVurderer(BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
    }

    public HarEndretKompletthetVurderer() {
    }


    boolean harKompletthetMedEndretVurdering(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        var grunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(input.getBehandlingReferanse().getBehandlingId());
        var kompletthetPeriode = grunnlag.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(Collections.emptyList())
            .stream().filter(k -> k.getSkjæringstidspunkt().equals(periode.getFomDato())).findFirst();

        var initiellVersjon = beregningPerioderGrunnlagRepository.getInitiellVersjon(input.getBehandlingReferanse().getBehandlingId());
        var initiellPeriode = initiellVersjon.map(BeregningsgrunnlagPerioderGrunnlag::getKompletthetPerioder).orElse(Collections.emptyList())
            .stream().filter(k -> k.getSkjæringstidspunkt().equals(periode.getFomDato())).findFirst();

        if (kompletthetPeriode.isEmpty()) {
            return initiellPeriode.isPresent();
        }

        if (initiellPeriode.isEmpty()) {
            return true;
        }

        return !kompletthetPeriode.get().getVurdering().equals(initiellPeriode.get().getVurdering());
    }


}
