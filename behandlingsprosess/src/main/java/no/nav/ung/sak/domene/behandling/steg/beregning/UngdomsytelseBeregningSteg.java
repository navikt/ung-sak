package no.nav.ung.sak.domene.behandling.steg.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.BeregnDagsatsInput;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.FødselOgDødInfo;
import no.nav.ung.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.util.List;
import java.util.function.Function;

@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.UNGDOMSYTELSE_BEREGNING)
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngdomsytelseBeregningSteg implements BehandlingSteg {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private VilkårTjeneste vilkårTjeneste;

    UngdomsytelseBeregningSteg() {
    }

    @Inject
    public UngdomsytelseBeregningSteg(BasisPersonopplysningTjeneste personopplysningTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                                      VilkårTjeneste vilkårTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(kontekst.getBehandlingId());
        var oppfyltVilkårTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.OPPFYLT)).mapValue(it -> true);
        if (oppfyltVilkårTidslinje.isEmpty()) {
            ungdomsytelseGrunnlagRepository.deaktiverSatsPerioder(kontekst.getBehandlingId());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var beregnDagsatsInput = lagInput(behandling, oppfyltVilkårTidslinje);
        var satsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(beregnDagsatsInput);
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), satsTidslinje);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private BeregnDagsatsInput lagInput(Behandling behandling, LocalDateTimeline<Boolean> oppfyltVilkårTidslinje) {
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(behandlingReferanse);
        var harTriggerBeregnHøySats = harProsesstriggerForBeregnHøySats(behandling);
        var harBeregnetHøySatsTidligere = harHøySatsIOriginalBehandling(behandling);

        var beregnDagsatsInput = new BeregnDagsatsInput(
            oppfyltVilkårTidslinje,
            personopplysningerAggregat.getSøker().getFødselsdato(),
            harTriggerBeregnHøySats,
            harBeregnetHøySatsTidligere,
            uledRelevantPersoninfo(personopplysningerAggregat));
        return beregnDagsatsInput;
    }

    private Boolean harHøySatsIOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId().flatMap(
                ungdomsytelseGrunnlagRepository::hentGrunnlag
            ).map(it -> it.getSatsPerioder().getPerioder().stream().anyMatch(p -> p.getSatsType().equals(UngdomsytelseSatsType.HØY)))
            .orElse(false);
    }

    private static boolean harProsesstriggerForBeregnHøySats(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream().anyMatch(it -> it.getBehandlingÅrsakType() == BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS);
    }

    private static List<FødselOgDødInfo> uledRelevantPersoninfo(PersonopplysningerAggregat personopplysningerAggregat) {
        var barnAvSøkerAktørId = personopplysningerAggregat.getRelasjoner()
            .stream().filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .toList();
        return barnAvSøkerAktørId.stream().map(mapFødselOgDødInformasjonForAktør(personopplysningerAggregat)).toList();
    }

    private static Function<AktørId, FødselOgDødInfo> mapFødselOgDødInformasjonForAktør(PersonopplysningerAggregat personopplysningerAggregat) {
        return aktørId -> new FødselOgDødInfo(aktørId, personopplysningerAggregat.getSøker().getFødselsdato(), personopplysningerAggregat.getSøker().getDødsdato());
    }


}
