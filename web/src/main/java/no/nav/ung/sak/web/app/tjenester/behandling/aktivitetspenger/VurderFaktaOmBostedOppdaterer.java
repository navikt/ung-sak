package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
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
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderFaktaOmBostedDto;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
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

    VurderFaktaOmBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBostedOppdaterer(BehandlingRepository behandlingRepository,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                         EtterlysningRepository etterlysningRepository,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        // Les eksisterende avklaringer per skjæringstidspunkt FØR lagreAvklaringer
        Map<LocalDate, BostedAvklaringData> tidligereAvklaringer = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslått().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    p -> p.getPeriode().getFomDato(),
                    p -> new BostedAvklaringData(p.isErBosattITrondheim(), p.getPeriode().getFomDato(), p.getFraflyttingsÅrsak(), p.getKilde())))).orElse(Map.of());

        // Bygg nye avklaringer basert på vurdering (nøkkel = vilkårsperiode fom)
        Map<Periode, BostedAvklaringData> nyeAvklaringer = new LinkedHashMap<>();
        for (BostedFaktaavklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            nyeAvklaringer.put(avklaring.periode(),
                BostedAvklaringUtil.tilAvklaringData(avklaring.periode().getFom(), avklaring.vurdering()));
        }

        // Lagre forseslått og ikke overføerer eksisterende resultat.
        Map<LocalDate, UUID> periodeReferanser = bostedsGrunnlagRepository.lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(behandlingId, nyeAvklaringer);

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

    private void opprettEtterlysning(VurderFaktaOmBostedDto dto, long behandlingId,
                                     Map<Periode, BostedAvklaringData> nyeAvklaringer,
                                     Map<LocalDate, BostedAvklaringData> tidligereAvklaringer,
                                     Map<LocalDate, UUID> periodeReferanser, Long fagsakId) {
        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();


        boolean skalAvbryte = false;
        boolean skalOpprette = false;
        List<BostedFaktaavklaringPeriodeDto> avklaringerSomKreverVarselVedEndring = dto.getAvklaringer().stream().filter(BostedFaktaavklaringPeriodeDto::skalSendeVarsel).toList();
        for (BostedFaktaavklaringPeriodeDto avklaring : avklaringerSomKreverVarselVedEndring) {
            LocalDate stp = avklaring.periode().getFom();

            BostedAvklaringData nyAvklaring = nyeAvklaringer.get(stp);
            BostedAvklaringData gammelAvklaring = tidligereAvklaringer.get(stp);
            Objects.requireNonNull(gammelAvklaring, "Dato for skjæringstidspunkt finnes ikke i liste over tidligere avklaringer. stp:" + stp);

            boolean avklaringEndret = erAvklaringEndret(nyAvklaring, gammelAvklaring);

            if (avklaringEndret) {
                // Avbryt aktiv
                var eksisterendeAktive = etterlysningerSomVenterSvar.stream()
                    .filter(e -> e.getPeriode().getFomDato().equals(stp))
                    .toList();
                skalAvbryte = skalAvbryte || !eksisterendeAktive.isEmpty();
                eksisterendeAktive.forEach(Etterlysning::skalAvbrytes);
                etterlysningRepository.lagre(eksisterendeAktive);

                // Opprett ny
                var etterlysning = Etterlysning.opprettForType(
                    behandlingId,
                    periodeReferanser.get(avklaring.periode().getFom()),
                    UUID.randomUUID(),
                    DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.periode().getFom(), avklaring.periode().getTom()),
                    EtterlysningType.UTTALELSE_BOSTED
                );
                skalOpprette = true;
                etterlysningRepository.lagre(etterlysning);

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
            nyAvklaring.fraflyttingsDato() != gammelAvklaring.fraflyttingsDato() ||
            nyAvklaring.fraflyttingsÅrsak() != gammelAvklaring.fraflyttingsÅrsak();
    }

}
