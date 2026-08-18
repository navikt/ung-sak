package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
                g.getForeslåttHvisEksisterer().map(f -> f.hentAvklaringerMedStatus(AvklaringStatus.AVKLARES))
            ).orElse(List.of());
    }

    public Set<BostedsPeriodeAvklaring> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(AksjonspunktOppdaterParameter param, List<BostedAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        var nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .toList();

        Set<BostedsPeriodeAvklaring> alleForeslåtteAvklaringerUnderArbeid = bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);

        var perioderSomSkalVurderesPåNytt = nyePeriodeAvklaringer.stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(behandlingId, param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);
        return alleForeslåtteAvklaringerUnderArbeid;
    }

    public void oppdaterEtterlysninger(Behandling behandling,
                                       Collection<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid,
                                       Collection<BostedsPeriodeAvklaring> alleGrunnlagsreferanserUnderArbeid) {

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

        Map<BostedAvklaringInnhold, UUID> avklaringerSomSkalVarsles = alleGrunnlagsreferanserUnderArbeid.stream()
            .filter(BostedsPeriodeAvklaring::skalSendeVarsel)
            .collect(Collectors.toMap(
                BostedsAvklaringDataMapper::mapTilBostedAvklaringInnhold,
                BostedsPeriodeAvklaring::getReferanse)
        );

        // Beholder kun nye avklaringer og avklaringer med endret innhold
        avklaringerSomSkalVarsles.keySet().removeAll(tidligereAvklaringer.keySet());

        var etterlysningerSomSkalAvbrytes = hentEtterlysningerForEndretEllerSlettetAvklaring(tidligereAvklaringer, avklaringerSomSkalVarsles, etterlysningerSomVenterSvar);
        etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);

        var nyeEtterlysninger = avklaringerSomSkalVarsles.entrySet().stream().map(avklaring ->
            Etterlysning.opprettForType(
                behandlingId,
                avklaring.getValue(),
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.getKey().periode().getFom(), avklaring.getKey().periode().getTom()),
                EtterlysningType.UTTALELSE_BOSTED
        )).toList();
        etterlysningRepository.lagre(nyeEtterlysninger);


        var skalAvbryte = etterlysningerSomSkalAvbrytes.stream().anyMatch(it -> it.getStatus() == EtterlysningStatus.AVBRUTT);
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

        var avklaringInneholdSomSkalVarsles = avklaringerSomSkalVarsles.keySet();
        var tidligereAvklaringerSomErEndretEllerIkkeLengerEksisterer = tidligereAvklaringerInnhold.entrySet().stream()
            .filter(entry -> avklaringInneholdSomSkalVarsles.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();

        return etterlysningerSomVenterSvar.stream()
            .filter(etterlysning -> tidligereAvklaringerSomErEndretEllerIkkeLengerEksisterer.contains(etterlysning.getGrunnlagsreferanse()))
            .peek(Etterlysning::setSkalAvbrytes)
            .toList();
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
}
