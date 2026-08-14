package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.AvklaringStatus;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.*;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBostedOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;


    VurderFaktaOmBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBostedOppdaterer(BehandlingRepository behandlingRepository,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                         EtterlysningRepository etterlysningRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                         InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType()).utled(behandlingId, VilkårType.BOSTEDSVILKÅR);
        var maxTomDato = perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Må ha perioder til vurdering"));

        List<BostedsPeriodeAvklaring> tidligereAvklaringerUnderArbeid = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g ->
                g.getForeslått().hentAvklaringerMedStatus(AvklaringStatus.AVKLARES)
            ).orElse(List.of());


        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        List<BostedAvklaringInnhold> nyeAvklaringer = dto.getAvklaringer().stream().filter(a -> a.vurdering() != null)
            .map(a -> BostedsAvklaringDataMapper.mapTilBostedAvklaringData(a, maxTomDato)).toList();

        Set<UUID> alleGrunnlagsreferanserUnderArbeid = lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(param, nyeAvklaringer, vurdertAv, vurdertTidspunkt, behandlingId);

        oppdaterEtterlysninger(behandling, nyeAvklaringer, tidligereAvklaringerUnderArbeid, alleGrunnlagsreferanserUnderArbeid);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Bostedsavklaring registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }

    private Set<UUID> lagreForeslåttAvklaringOgSettVilkårIkkeVurdert(AksjonspunktOppdaterParameter param, List<BostedAvklaringInnhold> nyeAvklaringer, String vurdertAv, LocalDateTime vurdertTidspunkt, long behandlingId) {
        var nyePeriodeAvklaringer = nyeAvklaringer.stream()
            .map(it -> BostedsAvklaringDataMapper.mapTilBostedsPeriodeAvklaring(it, vurdertAv, vurdertTidspunkt))
            .toList();

        Set<UUID> alleGrunnlagsreferanserUnderArbeid = bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);

        var perioderSomSkalVurderesPåNytt = nyePeriodeAvklaringer.stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(behandlingId, param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);
        return alleGrunnlagsreferanserUnderArbeid;
    }

    private void oppdaterEtterlysninger(Behandling behandling,
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
}
