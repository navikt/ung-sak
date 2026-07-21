package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedAvklaringData;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

        LocalDateTimeline<BostedAvklaringData> tidligereAvklaringer = hentTidligereAvklaringer(perioderTilVurdering, behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        Map<Periode, BostedAvklaringData> nyeAvklaringerSomSkalVarsles = new LinkedHashMap<>();
        List<BostedsPeriodeAvklaring> nyePeriodeAvklaringer = new ArrayList<>();
        for (BostedFaktaavklaringPeriodeDto avklaring : dto.getAvklaringer().stream().filter(a -> a.vurdering() != null).toList()) {
            var fom = avklaring.periode().getFom();
            var tom = avklaring.periode().getTom() != null ? avklaring.periode().getTom() : maxTomDato;

            var vurdering = avklaring.vurdering();
            if (avklaring.skalSendeVarsel()) {
                nyeAvklaringerSomSkalVarsles.put(new Periode(fom, tom), BostedAvklaringUtil.tilAvklaringData(fom, vurdering));
            }

            nyePeriodeAvklaringer.add(new BostedsPeriodeAvklaring(
                DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
                false,
                vurdering.fraflyttingsÅrsak(),
                vurdering.begrunnelse(),
                avklaring.skalSendeVarsel(),
                vurdering.fritekstTilVarsel(),
                vurdering.begrunnelseIkkeVarsel(),
                vurdertAv,
                vurdertTidspunkt
            ));
        }

        Map<LocalDate, UUID> periodeReferanser = bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);

        var perioderSomSkalVurderesPåNytt = nyePeriodeAvklaringer.stream().map(BostedsPeriodeAvklaring::getPeriode).toList();
        inngangsvilkårVurderingTjeneste.fjernVilkårVurderingOgSettVilkårResultatIkkeVurdertForPeriode(behandlingId, param.getVilkårResultatBuilder(), VilkårType.BOSTEDSVILKÅR, perioderSomSkalVurderesPåNytt);

        opprettEtterlysning(behandling, nyeAvklaringerSomSkalVarsles, tidligereAvklaringer, periodeReferanser);

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

    private LocalDateTimeline<BostedAvklaringData> hentTidligereAvklaringer(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, long behandlingId) {
        var perioderTilVurderingTidslinje = new LocalDateTimeline<>(perioderTilVurdering.stream().map(periode-> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true)).toList());

        return bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g ->
                g.hentOppgittOgForeslåttFaktaSomTidslinje()
                    .intersection(perioderTilVurderingTidslinje).map(p ->
                        List.of(new LocalDateSegment<>(p.getLocalDateInterval(), new BostedAvklaringData(p.getValue().isErBosattITrondheim(), p.getValue().isErBosattITrondheim() ? null : p.getFom(), p.getValue().getIkkeOppfyltÅrsak(), p.getValue().getKilde())))
                 ))
            .orElse(LocalDateTimeline.empty());
    }

    private void opprettEtterlysning(Behandling behandling,
                                     Map<Periode, BostedAvklaringData> nyeAvklaringerSomSkalVarsles,
                                     LocalDateTimeline<BostedAvklaringData> tidligereTidslinje,
                                     Map<LocalDate, UUID> periodeReferanser) {

        long behandlingId = behandling.getId();

        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();

        boolean skalAvbryte = false;
        boolean skalOpprette = false;

        for (var avklaringSomKreverVarsel : nyeAvklaringerSomSkalVarsles.entrySet()) {
            BostedAvklaringData nyAvklaring = avklaringSomKreverVarsel.getValue();
            Periode periode = avklaringSomKreverVarsel.getKey();

            LocalDateTimeline<BostedAvklaringData> tidligereOverlappendeAvklaringerTidslinje = tidligereTidslinje.intersection(new LocalDateInterval(periode.getFom(), periode.getTom()));
            if (tidligereOverlappendeAvklaringerTidslinje.isEmpty()) {
                skalOpprette = true;
            } else {
                List<BostedAvklaringData> gamleAvklaringerSomOverlapperMedDenne = tidligereOverlappendeAvklaringerTidslinje.stream()
                    .map(LocalDateSegment::getValue).toList();

                boolean avklaringEndret = gamleAvklaringerSomOverlapperMedDenne.stream().anyMatch(gammelAvklaring -> erAvklaringEndret(nyAvklaring, gammelAvklaring));

                if (avklaringEndret) {
                    // Avbryt aktiv
                    var eksisterendeAktive = etterlysningerSomVenterSvar.stream()
                        .filter(e -> e.getPeriode().tilPeriode().overlaps(periode))
                        .toList();
                    skalAvbryte = skalAvbryte || !eksisterendeAktive.isEmpty();
                    eksisterendeAktive.forEach(Etterlysning::setSkalAvbrytes);
                    etterlysningRepository.lagre(eksisterendeAktive);

                    // Opprett ny
                    var etterlysning = Etterlysning.opprettForType(
                        behandlingId,
                        periodeReferanser.get(periode.getFom()),
                        UUID.randomUUID(),
                        DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()),
                        EtterlysningType.UTTALELSE_BOSTED
                    );
                    skalOpprette = true;
                    etterlysningRepository.lagre(etterlysning);
                }
            }
        }

        long fagsakId = behandling.getFagsakId();
        if (skalAvbryte) {
            var task = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            task.setBehandling(fagsakId, behandlingId);
            prosessTaskTjeneste.lagre(task);
        }
        if (skalOpprette) {
            var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
            task.setBehandling(fagsakId, behandlingId);
            task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_BOSTED.getKode());
            prosessTaskTjeneste.lagre(task);
        }
    }

    private static boolean erAvklaringEndret(BostedAvklaringData nyAvklaring, BostedAvklaringData gammelAvklaring) {
        return nyAvklaring.erBosattITrondheim() != gammelAvklaring.erBosattITrondheim() ||
            !Objects.equals(nyAvklaring.fraflyttingsDato(), gammelAvklaring.fraflyttingsDato()) ||
             nyAvklaring.fraflyttingsÅrsak() != gammelAvklaring.fraflyttingsÅrsak();
    }

}
