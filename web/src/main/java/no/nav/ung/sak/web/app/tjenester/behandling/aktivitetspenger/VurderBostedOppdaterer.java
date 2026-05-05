package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
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
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedAvklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderBostedDto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBostedOppdaterer implements AksjonspunktOppdaterer<VurderBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    VurderBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBostedOppdaterer(BehandlingRepository behandlingRepository,
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
    public OppdateringResultat oppdater(VurderBostedDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        long behandlingId = behandling.getId();

        // Les eksisterende avklaringer per skjæringstidspunkt FØR lagreAvklaringer
        Map<LocalDate, BostedAvklaringData> tidligereAvklaringer = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getHolder().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                    p -> new BostedAvklaringData(p.isErBosattITrondheim(), p.getFraflyttingsDato(), p.getFraflyttingsÅrsak(), p.getKilde())))).orElse(Map.of());

        // Bygg nye avklaringer basert på vurdering (nøkkel = vilkårsperiode fom)
        Map<LocalDate, BostedAvklaringData> nyeAvklaringer = new LinkedHashMap<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            nyeAvklaringer.put(avklaring.periode().getFom(),
                BostedAvklaringUtil.tilAvklaringData(avklaring.periode().getFom(), avklaring.vurdering()));
        }

        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        List<Etterlysning> etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .toList();

        Map<LocalDate, UUID> periodeReferanser = bostedsGrunnlagRepository.lagreAvklaringer(behandlingId, nyeAvklaringer);

        boolean skalAvbryte = false;
        boolean skalOpprette = false;
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            LocalDate stp = avklaring.periode().getFom();

            BostedAvklaringData nyAvklaring = nyeAvklaringer.get(stp);
            BostedAvklaringData gammelAvklaring = tidligereAvklaringer.get(stp);
            boolean avklaringEndret = !nyAvklaring.equals(gammelAvklaring);

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
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(task);
        }
        if (skalOpprette) {
            var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_BOSTED.getKode());
            prosessTaskTjeneste.lagre(task);
        }

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Bostedsavklaring registrert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.setSteg(BehandlingStegType.VURDER_BOSTED);
        resultat.rekjørSteg();
        return resultat;
    }

}
