package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.perioder.ForlengelseTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.registerendringer.EndringerIAY;
import no.nav.k9.sak.registerendringer.RelevanteIAYRegisterendringerUtleder;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class PleiepengerRelevanteIAYRegisterendringerUtleder implements RelevanteIAYRegisterendringerUtleder {

    private ForlengelseTjeneste forlengelseTjeneste;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private BehandlingRepository behandlingRepository;

    private VilkårTjeneste vilkårTjeneste;

    public PleiepengerRelevanteIAYRegisterendringerUtleder() {
        // CDI
    }

    @Inject
    public PleiepengerRelevanteIAYRegisterendringerUtleder(ForlengelseTjeneste forlengelseTjeneste,
                                                           VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, BehandlingRepository behandlingRepository, VilkårTjeneste vilkårTjeneste) {
        this.forlengelseTjeneste = forlengelseTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }


    @Override
    public EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse) {


        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId().orElseThrow();
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);

        var originalVilkår = vilkårTjeneste.hentVilkårResultat(originalBehandlingId);

        var originalVilkårTidslinje = vilkårTjeneste.samletVilkårsresultat(originalBehandlingId);

        var gjeldendeVilkårTidslinje = vilkårTjeneste.samletVilkårsresultat(originalBehandlingId);


        var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType())
            .utled(behandlingReferanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());

        var originalIAYGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(originalBehandlingId);



        if (originalIAYGrunnlag.isEmpty()) {
            throw new IllegalStateException("Fant ikke iay fra original behandling");

        }

        var aktørArbeidFraRegister = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId());


        var perioderMedMarkering = vilkårPeriodeFilterProvider.getFilter(behandlingReferanse).filtrerPerioder(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);


        forlengelseTjeneste.utledPerioderSomSkalBehandlesSomForlengelse(behandlingReferanse, pe)
        return null;
    }
}
