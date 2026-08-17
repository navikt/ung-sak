package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
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
            .map(g ->
                g.getForeslått().hentAvklaringerMedStatus(AvklaringStatus.AVKLARES)
            ).orElse(List.of());
    }

    public Set<UUID> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(AksjonspunktOppdaterParameter param, List<BostedAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        var nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .toList();

        Set<UUID> alleGrunnlagsreferanserUnderArbeid = bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);

        var perioderSomSkalVurderesPåNytt = nyePeriodeAvklaringer.stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(behandlingId, param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);
        return alleGrunnlagsreferanserUnderArbeid;
    }

    public void oppdaterEtterlysninger(Behandling behandling,
                                        List<BostedAvklaringInnhold> nyeAvklaringInnhold,
                                        List<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid,
                                        Set<UUID> alleGrunnlagsreferanserUnderArbeid) {

        long behandlingId = behandling.getId();

        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();

        var avklaringerSomSkalVarsles = nyeAvklaringInnhold.stream()
            .filter(BostedAvklaringInnhold::skalSendeVarsel)
            .collect(Collectors.toSet());

        var tidligereAvklaringerInnhold = tidligereAvklaringerUnderArbeid.stream()
            .collect(Collectors.toMap(
                BostedsAvklaringDataMapper::mapTilBostedAvklaringData,
                it -> it,
                (første, _) -> første,
                LinkedHashMap::new));

        // Beholder kun nye avklaringer og avklaringer med endret innhold
        avklaringerSomSkalVarsles.removeAll(tidligereAvklaringerInnhold.keySet());

        var avklaringsreferanserSomErEndret = tidligereAvklaringerInnhold.entrySet().stream().map(entry -> {
            var tidligereAvklaringInnhold = entry.getKey();
            var tidligereBostedsPeriodeAvklaring = entry.getValue();

            if (!avklaringerSomSkalVarsles.contains(tidligereAvklaringInnhold)) {
                return tidligereBostedsPeriodeAvklaring.getReferanse();
            }
            return null;
        }).filter(Objects::nonNull).toList();

        var eksisterendeVarsler = etterlysningerSomVenterSvar.stream()
            .filter(etterlysning -> !alleGrunnlagsreferanserUnderArbeid.contains(etterlysning.getGrunnlagsreferanse()) || avklaringsreferanserSomErEndret.contains(etterlysning.getGrunnlagsreferanse()))
            .peek(Etterlysning::setSkalAvbrytes)
            .toList();

        etterlysningRepository.lagre(eksisterendeVarsler);

        var skalAvbryte = eksisterendeVarsler.stream().anyMatch(it -> it.getStatus() == EtterlysningStatus.AVBRUTT);
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

    @Override
    public void settAlleAvklaringerTilFerdig(long behandlingId) {
        bostedsGrunnlagRepository.lagre(behandlingId, grunnlag -> {
            if (grunnlag.getForeslått() != null) {
                grunnlag.getForeslått().settAlleAvklaringerTilFerdig();
            }
        });
    }
}
