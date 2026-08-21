package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.AvklaringStatus;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringUnderArbeid;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class BostedAvklaringTjeneste implements VilkårsavklaringTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BostedAvklaringTjeneste.class);

    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    public BostedAvklaringTjeneste() {
    }

    @Inject
    public BostedAvklaringTjeneste(BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                   InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                   EtterlysningRepository etterlysningRepository,
                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public List<BostedsPeriodeAvklaring> hentBostedPeriodeAvklaringUnderArbeid(long behandlingId) {
        return bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslåtteAvklaringerMedStatus(AvklaringStatus.UNDER_ARBEID))
            .orElse(List.of());
    }

    public Set<BostedsPeriodeAvklaring> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(List<BostedAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        var nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .collect(Collectors.toSet());
        return bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);
    }

    public void oppdaterEtterlysninger(Behandling behandling,
                                       Collection<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid,
                                       Collection<BostedsPeriodeAvklaring> alleAvklaringerUnderArbeid) {

        long behandlingId = behandling.getId();

        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();

        Map<BostedAvklaringInnhold, UUID> tidligereAvklaringer = tidligereAvklaringerUnderArbeid.stream()
            .collect(Collectors.toMap(
                BostedsAvklaringDataMapper::mapTilBostedAvklaringInnhold,
                BostedsPeriodeAvklaring::getReferanse
            ));

        Map<BostedAvklaringInnhold, UUID> avklaringerSomSkalVarsles = alleAvklaringerUnderArbeid.stream()
            .filter(BostedsPeriodeAvklaring::skalSendeVarsel)
            .collect(Collectors.toMap(
                BostedsAvklaringDataMapper::mapTilBostedAvklaringInnhold,
                BostedsPeriodeAvklaring::getReferanse)
            );

        var referanserForUendretInnhold = tidligereAvklaringer.entrySet().stream()
            .filter(entry -> avklaringerSomSkalVarsles.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        var etterlysningerSomSkalAvbrytes = etterlysningerSomVenterSvar.stream()
            .filter(etterlysning -> !referanserForUendretInnhold.contains(etterlysning.getGrunnlagsreferanse()))
            .peek(Etterlysning::setSkalAvbrytes)
            .toList();
        etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);

        // Beholder kun nye avklaringer og avklaringer med endret innhold
        avklaringerSomSkalVarsles.keySet().removeAll(tidligereAvklaringer.keySet());
        var nyeEtterlysninger = avklaringerSomSkalVarsles.entrySet().stream().map(avklaring ->
            Etterlysning.opprettForType(
                behandlingId,
                avklaring.getValue(),
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.getKey().periode().getFom(), avklaring.getKey().periode().getTom()),
                EtterlysningType.UTTALELSE_BOSTED
        )).toList();
        etterlysningRepository.lagre(nyeEtterlysninger);


        var skalAvbryte = etterlysningerSomSkalAvbrytes.stream().anyMatch(it -> it.getStatus() == EtterlysningStatus.SKAL_AVBRYTES);
        if (skalAvbryte) {
            log.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            var task = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(task);
        }

        if (!avklaringerSomSkalVarsles.isEmpty()) {
            log.info("Oppretter etterlysninger {}", avklaringerSomSkalVarsles);
            var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_BOSTED.getKode());
            prosessTaskTjeneste.lagre(task);
        }
    }

    // Hvis saksbehandler endrer perioden det avklares for etter at vilkårsvurdering er utført, gjelder ikke lenger vurderingen og den delen som ikke overlapper med ny avklaring må gjenopprettes fra forrige behandling.
    // Vilkårsperioden som avklaringen gjelder for settes til ikke vurdert, slik at den kan vurderes på nytt (automatisk eller i aksjonspunkt)
    public void gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(AksjonspunktOppdaterParameter param, List<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid, List<BostedAvklaringInnhold> nyeAvklaringer) {
        var tidligereTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(tidligereAvklaringerUnderArbeid.stream().map(BostedsPeriodeAvklaring::getPeriode).toList());
        var nyTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(nyeAvklaringer.stream().map(BostedAvklaringInnhold::periode).toList());
        var tidslinjeSomIkkeHåndteresAvNyAvklaring = tidligereTidslinje.disjoint(nyTidslinje);

        inngangsvilkårVurderingTjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(param.getBehandlingId(), param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, tidslinjeSomIkkeHåndteresAvNyAvklaring);
        inngangsvilkårVurderingTjeneste.oppdaterBostedsvilkårResultatFraVurdering(param.getBehandlingId(), param.getVilkårResultatBuilder());

        var perioderSomSkalVurderesPåNytt = TidslinjeUtil.tilDatoIntervallEntiteter(nyTidslinje);
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);
    }

    @Override
    public void settAlleAvklaringerTilFerdig(long behandlingId) {
        bostedsGrunnlagRepository.settAlleAvklaringerFerdig(behandlingId);
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = hentBostedPeriodeAvklaringUnderArbeid(behandlingId).stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VilkårType.BOSTEDSVILKÅR, perioderTidligereVurdertEtterAvklaring);
    }

    @Override
    public boolean gjelderFor(BehandlingÅrsakType behandlingÅrsakType) {
        return BehandlingÅrsakType.ENDRET_BOSTED.equals(behandlingÅrsakType);
    }

    @Override
    public Optional<VilkårsavklaringUnderArbeid> hentSenesteAvklaringUnderArbeid(long behandlingId) {
        return hentBostedPeriodeAvklaringUnderArbeid(behandlingId).stream()
            .max(Comparator.comparing(BostedsPeriodeAvklaring::getVurdertTidspunkt))
            .map(avklaring -> new VilkårsavklaringUnderArbeid(avklaring.getAvklaringtype(), avklaring.getPeriode()));
    }

}
