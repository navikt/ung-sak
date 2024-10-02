package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndringPåForlengelseInput;
import no.nav.k9.sak.perioder.EndringPåForlengelsePeriodeVurderer;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.HarInntektsmeldingerRelevanteEndringerForPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.PSBEndringPåForlengelseInput;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
@ApplicationScoped
public class PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer implements EndringPåForlengelsePeriodeVurderer {

    private static final Set<BehandlingÅrsakType> RELEVANTE_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);

    private ProsessTriggereRepository prosessTriggereRepository;
    private HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode;


    PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer() {
    }

    @Inject
    public PleiepengerOpptjeningEndringPåForlengelsePeriodeVurderer(ProsessTriggereRepository prosessTriggereRepository,
                                                                    HarInntektsmeldingerRelevanteEndringerForPeriode harInntektsmeldingerRelevanteEndringerForPeriode) {
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.harInntektsmeldingerRelevanteEndringerForPeriode = harInntektsmeldingerRelevanteEndringerForPeriode;
    }

    @Override
    public boolean harPeriodeEndring(EndringPåForlengelseInput input, DatoIntervallEntitet periode) {
        var inntektsmeldinger = ((PSBEndringPåForlengelseInput) input).getSakInntektsmeldinger();
        var harEndringer = !harInntektsmeldingerRelevanteEndringerForPeriode.finnInntektsmeldingerMedRelevanteEndringerForPeriode(inntektsmeldinger, input.getBehandlingReferanse(), periode, VilkårType.OPPTJENINGSVILKÅRET).isEmpty();
        if (harEndringer) {
            return true;
        }
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
