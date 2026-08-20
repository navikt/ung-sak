package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
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
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårAvklaringOppdaterer;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class BostedAvklaringTjeneste implements VilkårAvklaringOppdaterer {

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
            .flatMap(g ->
                g.getForeslåttHvisEksisterer().map(f -> f.hentAvklaringerMedStatus(AvklaringStatus.UNDER_ARBEID))
            ).orElse(List.of());
    }

    public Set<BostedsPeriodeAvklaring> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(List<BostedAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        var nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .toList();
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

        var etterlysningerSomSkalAvbrytes = hentEtterlysningerForEndretEllerSlettetAvklaring(tidligereAvklaringer, avklaringerSomSkalVarsles, etterlysningerSomVenterSvar);
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
            var task = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(task);
        }

        if (!avklaringerSomSkalVarsles.isEmpty()) {
            var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_BOSTED.getKode());
            prosessTaskTjeneste.lagre(task);
        }
    }

    private static List<Etterlysning> hentEtterlysningerForEndretEllerSlettetAvklaring(Map<BostedAvklaringInnhold, UUID> tidligereAvklaringerInnhold,
                                                                                       Map<BostedAvklaringInnhold, UUID> avklaringerSomSkalVarsles,
                                                                                       List<Etterlysning> etterlysningerSomVenterSvar) {

        var referanserForUendretInnhold = tidligereAvklaringerInnhold.entrySet().stream()
            .filter(entry -> avklaringerSomSkalVarsles.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        return etterlysningerSomVenterSvar.stream()
            .filter(etterlysning -> !referanserForUendretInnhold.contains(etterlysning.getGrunnlagsreferanse()))
            .peek(Etterlysning::setSkalAvbrytes)
            .toList();
    }

    // Hvis saksbehandler endrer perioden det avklares for etter at vilkårsvurdering er utført, gjelder ikke lenger vurderingen og den delen som ikke overlapper med ny avklaring må gjenopprettes fra forrige behandling.
    // Vilkårsperioden som avklaringen gjelder for settes til ikke vurdert, slik at den kan vurderes på nytt (automatisk eller i aksjonspunkt)
    public void gjenopprettTidligereVilkårsvurderingVedBehovOgSettAvklartPeriodeTilIkkeVurdert(AksjonspunktOppdaterParameter param, List<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid, List<BostedAvklaringInnhold> nyeAvklaringer) {
        var tidligereTidslinje = new LocalDateTimeline<>(tidligereAvklaringerUnderArbeid.stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), true)).toList());
        var nyTidslinje = new LocalDateTimeline<>(nyeAvklaringer.stream().map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true)).toList());
        var periodeSomIkkeHåndteresAvNyAvklaring = tidligereTidslinje.disjoint(nyTidslinje)
            .segmenter().stream()
            .map(it -> new Periode(it.getFom(), it.getTom()))
            .toList();

        inngangsvilkårVurderingTjeneste.gjenopprettForrigeVurderingForPerioderIkkeVurdert(param.getBehandlingId(), param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, periodeSomIkkeHåndteresAvNyAvklaring);
        inngangsvilkårVurderingTjeneste.oppdaterBostedsvilkårResultatFraVurdering(param.getBehandlingId(), param.getVilkårResultatBuilder());

        var perioderSomSkalVurderesPåNytt = nyTidslinje.stream().map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval())).toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);
    }

    @Override
    public void settAlleAvklaringerTilFerdig(long behandlingId) {
        if (bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId).isEmpty()) {
            // behandlingen har ikke bostedsgrunnlag (f.eks. annen ytelse) - ikke opprett et tomt
            return;
        }
        bostedsGrunnlagRepository.lagre(behandlingId, grunnlag -> {
            if (grunnlag.getForeslått() != null) {
                grunnlag.getForeslått().settAlleAvklaringerTilFerdig();
            }
        });
    }

    @Override
    public void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId) {
        var perioderTidligereVurdertEtterAvklaring = hentBostedPeriodeAvklaringUnderArbeid(behandlingId).stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.settVilkårResultatIkkeVurdertForPeriode(behandlingId, VilkårType.BOSTEDSVILKÅR, perioderTidligereVurdertEtterAvklaring);
    }

}
