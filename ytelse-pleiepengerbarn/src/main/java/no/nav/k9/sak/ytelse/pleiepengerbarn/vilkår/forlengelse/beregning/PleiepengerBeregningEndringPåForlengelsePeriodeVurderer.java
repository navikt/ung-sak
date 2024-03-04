package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.HarEndretInntektsmeldingVurderer;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;
import no.nav.k9.sak.perioder.EndringPåForlengelsePeriodeVurderer;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.PSBEndringPåForlengelseInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.HarInntektsmeldingerRelevanteEndringerForPeriode;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
@ApplicationScoped
public class PleiepengerBeregningEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer {

    private static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_SATS_REGULERING,
        BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG,
        BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT,
        BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT,
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG);

    private ProsessTriggereRepository prosessTriggereRepository;

    private Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere;
    private HarEndretKompletthetVurderer harEndretKompletthetVurderer;
    private HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode;



    PleiepengerBeregningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public PleiepengerBeregningEndringPåForlengelsePeriodeVurderer(ProsessTriggereRepository prosessTriggereRepository,
                                                                   @Any Instance<EndringPåForlengelsePeriodeVurderer> endringsVurderere,
                                                                   HarEndretKompletthetVurderer harEndretKompletthetVurderer,
                                                                   HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode) {

        this.prosessTriggereRepository = prosessTriggereRepository;
        this.endringsVurderere = endringsVurderere;
        this.harEndretKompletthetVurderer = harEndretKompletthetVurderer;
        this.harInntektsmeldingerRelevanteEndringerForPeriode = harInntektsmeldingerRelevanteEndringerForPeriode;
    }

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        if (harMarkertPeriodeForReberegning(input, periode)) {
            return true;
        }

        var inntektsmeldinger = ((PSBEndringPåForlengelseInput) input).getSakInntektsmeldinger();
        var harEndringer = !harInntektsmeldingerRelevanteEndringerForPeriode.finnInntektsmeldingerMedRelevanteEndringerForPeriode(inntektsmeldinger, input.getBehandlingReferanse(), periode, VilkårType.BEREGNINGSGRUNNLAGVILKÅR).isEmpty();
        if (harEndringer) {
            return true;
        }

        if (harEndretKompletthetVurderer.harKompletthetMedEndretVurdering(input, periode)) {
            return true;
        }

        var vurderer = EndringPåForlengelsePeriodeVurderer.finnVurderer(endringsVurderere, VilkårType.OPPTJENINGSVILKÅRET, input.getBehandlingReferanse().getFagsakYtelseType());

        return vurderer.harPeriodeEndring(input, periode);
    }

    private boolean harMarkertPeriodeForReberegning(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        var prosessTriggereOpt = prosessTriggereRepository.hentGrunnlag(input.getBehandlingReferanse().getBehandlingId());

        if (prosessTriggereOpt.isPresent()) {
            var aktuelleTriggere = prosessTriggereOpt.get()
                .getTriggere()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .filter(it -> RELEVANTE_ÅRSAKER.contains(it.getÅrsak()))
                .toList();

            return !aktuelleTriggere.isEmpty();
        }
        return false;
    }

}
