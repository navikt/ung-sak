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
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
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
        validerKunVurdertePerioder(samletResultat);
        var oppfyltVilkårTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.OPPFYLT)).mapValue(it -> true);
        if (oppfyltVilkårTidslinje.isEmpty()) {
            ungdomsytelseGrunnlagRepository.deaktiverGrunnlag(kontekst.getBehandlingId());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var beregnDagsatsInput = lagInput(behandling, oppfyltVilkårTidslinje);
        var satsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(beregnDagsatsInput);
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), satsTidslinje);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /**
         * Validerer at alle perioder i tidslinjen er vurdert.
         * Kaster IllegalStateException hvis noen periode ikke er vurdert.
         *
         * @param samletResultat Tidslinje med vurderte vilkår
         */
    private static void validerKunVurdertePerioder(LocalDateTimeline<VilkårUtfallSamlet> samletResultat) {
        var ikkeVurdertTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.IKKE_VURDERT)).mapValue(it -> true);

        if (!ikkeVurdertTidslinje.isEmpty()){
            throw new IllegalStateException("Fant segmenter som ikke var vurdert: " + ikkeVurdertTidslinje.getLocalDateIntervals());
        }
    }

    /**
     * Lager input til dagsatsberegning basert på behandling og oppfylt vilkår-tidslinje.
     * @param behandling Behandlingen det skal beregnes for
     * @param oppfyltVilkårTidslinje Tidslinje for oppfylte vilkår
     * @return BeregnDagsatsInput med relevante parametre
     */
    private BeregnDagsatsInput lagInput(Behandling behandling, LocalDateTimeline<Boolean> oppfyltVilkårTidslinje) {
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(behandlingReferanse);
        return new BeregnDagsatsInput(
            oppfyltVilkårTidslinje,
            personopplysningerAggregat.getSøker().getFødselsdato(),
            harProsesstriggerForBeregnHøySats(behandling),
            harHøySatsIOriginalBehandling(behandling),
            utledBarnsFødselOgDødInformasjon(personopplysningerAggregat)
        );
    }

    /**
     * Sjekker om det finnes høy sats i original behandling.
     * @param behandling Behandlingen som skal sjekkes
     * @return true hvis minst én periode har høy sats, ellers false
     */
    private boolean harHøySatsIOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .flatMap(ungdomsytelseGrunnlagRepository::hentGrunnlag)
            .map(grunnlag -> grunnlag.getSatsPerioder().getPerioder().stream()
                .anyMatch(p -> p.getSatsType() == UngdomsytelseSatsType.HØY))
            .orElse(false);
    }

    /**
     * Sjekker om det finnes prosesstrigger for beregning av høy sats.
     */
    private static boolean harProsesstriggerForBeregnHøySats(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .anyMatch(a -> a.getBehandlingÅrsakType() == BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS);
    }

    /**
     * Henter ut fødsels- og dødsinformasjon for alle barn av søker.
     * @param personopplysningerAggregat Personopplysninger for behandlingen
     * @return Liste med FødselOgDødInfo for alle barn
     */
    private static List<FødselOgDødInfo> utledBarnsFødselOgDødInformasjon(PersonopplysningerAggregat personopplysningerAggregat) {
        return personopplysningerAggregat.getRelasjoner().stream()
            .filter(r -> r.getRelasjonsrolle() == RelasjonsRolleType.BARN)
            .map(PersonRelasjonEntitet::getTilAktørId)
            .map(mapFødselOgDødInformasjonForAktør(personopplysningerAggregat))
            .toList();
    }

    private static Function<AktørId, FødselOgDødInfo> mapFødselOgDødInformasjonForAktør(PersonopplysningerAggregat personopplysningerAggregat) {
        return aktørId -> new FødselOgDødInfo(aktørId, personopplysningerAggregat.getPersonopplysning(aktørId).getFødselsdato(), personopplysningerAggregat.getPersonopplysning(aktørId).getDødsdato());
    }


}
