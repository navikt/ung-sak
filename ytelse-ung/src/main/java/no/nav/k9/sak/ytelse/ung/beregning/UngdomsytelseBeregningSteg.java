package no.nav.k9.sak.ytelse.ung.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.UNGDOMSYTELSE_BEREGNING)
@BehandlingTypeRef
public class UngdomsytelseBeregningSteg implements BehandlingSteg {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsytelseBeregnDagsats beregnDagsatsTjeneste;
    private VilkårTjeneste vilkårTjeneste;

    UngdomsytelseBeregningSteg() {
    }

    @Inject
    public UngdomsytelseBeregningSteg(BasisPersonopplysningTjeneste personopplysningTjeneste,
                                      @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE) VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository, UngdomsytelseBeregnDagsats beregnDagsatsTjeneste, VilkårTjeneste vilkårTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.beregnDagsatsTjeneste = beregnDagsatsTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var perioder = vilkårsPerioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.UNGDOMSPROGRAMVILKÅRET);
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(kontekst.getBehandlingId());
        var perioderTilVurderingTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioder);
        var oppfyltVilkårTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.OPPFYLT));
        var innvilgetPerioderTidslinje = perioderTilVurderingTidslinje.intersection(oppfyltVilkårTidslinje, StandardCombinators::leftOnly);
        if (innvilgetPerioderTidslinje.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), behandling.getFagsak().getPeriode().getFomDato());
        var fødselsdato = personopplysningerAggregat.getSøker().getFødselsdato();
        var satsTidslinje = beregnDagsatsTjeneste.beregnDagsats(innvilgetPerioderTidslinje, fødselsdato);
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), satsTidslinje);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
