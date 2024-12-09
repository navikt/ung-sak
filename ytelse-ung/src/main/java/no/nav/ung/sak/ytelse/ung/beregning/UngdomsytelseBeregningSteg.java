package no.nav.ung.sak.ytelse.ung.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.time.LocalDate;

@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.UNGDOMSYTELSE_BEREGNING)
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
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
                                      UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                                      UngdomsytelseBeregnDagsats beregnDagsatsTjeneste,
                                      VilkårTjeneste vilkårTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.beregnDagsatsTjeneste = beregnDagsatsTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var perioder = vilkårsPerioderTilVurderingTjeneste.utled(kontekst.getBehandlingId());
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
        var beregningsdato = LocalDate.now();
        var harTriggerBeregnHøySats = behandling.getBehandlingÅrsaker().stream().anyMatch(it->it.getBehandlingÅrsakType() == BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS);
        var satsTidslinje = beregnDagsatsTjeneste.beregnDagsats(BehandlingReferanse.fra(behandling), innvilgetPerioderTidslinje, fødselsdato, beregningsdato, harTriggerBeregnHøySats);
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), satsTidslinje);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
