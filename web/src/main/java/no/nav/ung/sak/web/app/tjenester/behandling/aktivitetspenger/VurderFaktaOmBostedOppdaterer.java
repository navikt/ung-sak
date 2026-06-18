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
import no.nav.ung.kodeverk.bosatt.Kilde;
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


    VurderFaktaOmBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBostedOppdaterer(BehandlingRepository behandlingRepository,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                         EtterlysningRepository etterlysningRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType()).utled(behandlingId, VilkårType.BOSTEDSVILKÅR);
        LocalDateTimeline<BostedAvklaringData> tidligereAvklaringer = hentTidligereAvklaringer(perioderTilVurdering, behandlingId);

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        Map<Periode, BostedAvklaringData> nyeAvklaringer = new LinkedHashMap<>();
        List<BostedsPeriodeAvklaring> nyePeriodeAvklaringer = new ArrayList<>();
        for (BostedFaktaavklaringPeriodeDto avklaring : dto.getAvklaringer().stream().filter(a -> a.vurdering() != null).toList()) {
            nyeAvklaringer.put(avklaring.periode(), BostedAvklaringUtil.tilAvklaringData(avklaring.periode().getFom(), avklaring.vurdering()));

            nyePeriodeAvklaringer.add(new BostedsPeriodeAvklaring(
                DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.periode().getFom(), avklaring.periode().getTom()),
                false,
                avklaring.vurdering().fraflyttingsÅrsak(),
                vurdertAv,
                vurdertTidspunkt
            ));
        }

        Map<LocalDate, UUID> periodeReferanser = bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, nyePeriodeAvklaringer);

        opprettEtterlysning(dto, behandlingId, nyeAvklaringer, tidligereAvklaringer, periodeReferanser, behandling.getFagsakId());

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

    private void opprettEtterlysning(VurderFaktaOmBostedDto dto, long behandlingId,
                                     Map<Periode, BostedAvklaringData> nyeAvklaringer,
                                     LocalDateTimeline<BostedAvklaringData> tidligereTidslinje,
                                     Map<LocalDate, UUID> periodeReferanser, Long fagsakId) {
        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();


        boolean skalAvbryte = false;
        boolean skalOpprette = false;
        List<BostedFaktaavklaringPeriodeDto> avklaringerSomKreverVarselVedEndring = dto.getAvklaringer().stream().filter(BostedFaktaavklaringPeriodeDto::skalSendeVarsel).toList();
        for (BostedFaktaavklaringPeriodeDto avklaringSomKreverVarsel : avklaringerSomKreverVarselVedEndring) {
            LocalDate fom = avklaringSomKreverVarsel.periode().getFom();
            LocalDate tom = avklaringSomKreverVarsel.periode().getTom();

            BostedAvklaringData nyAvklaring = nyeAvklaringer.get(avklaringSomKreverVarsel.periode());

            LocalDateTimeline<BostedAvklaringData> gammelAvklaringTidslinje = tidligereTidslinje.intersection(new LocalDateInterval(fom, tom));
            if (gammelAvklaringTidslinje.isEmpty()) {
                skalOpprette = true;
            } else {
                BostedAvklaringData gammelAvklaring = gammelAvklaringTidslinje.stream()
                    .map(LocalDateSegment::getValue)
                    .findFirst().orElseThrow();

                boolean avklaringEndret = erAvklaringEndret(nyAvklaring, gammelAvklaring);

                if (avklaringEndret) {
                    // Avbryt aktiv
                    var eksisterendeAktive = etterlysningerSomVenterSvar.stream()
                        .filter(e -> e.getPeriode().getFomDato().equals(fom))
                        .toList();
                    skalAvbryte = skalAvbryte || !eksisterendeAktive.isEmpty();
                    eksisterendeAktive.forEach(Etterlysning::skalAvbrytes);
                    etterlysningRepository.lagre(eksisterendeAktive);

                    // Opprett ny
                    var etterlysning = Etterlysning.opprettForType(
                        behandlingId,
                        periodeReferanser.get(avklaringSomKreverVarsel.periode().getFom()),
                        UUID.randomUUID(),
                        DatoIntervallEntitet.fraOgMedTilOgMed(avklaringSomKreverVarsel.periode().getFom(), avklaringSomKreverVarsel.periode().getTom()),
                        EtterlysningType.UTTALELSE_BOSTED
                    );
                    skalOpprette = true;
                    etterlysningRepository.lagre(etterlysning);

                }
            }
        }

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
