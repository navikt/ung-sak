package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedAvklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderBostedDto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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

        Map<LocalDate, Boolean> avklaringerPerSkjæringstidspunkt = new LinkedHashMap<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            avklaringerPerSkjæringstidspunkt.put(avklaring.getPeriode().getFom(), avklaring.getErBosattITrondheim());
        }

        // Les eksisterende foreslått avklaring per fom BEFORE lagreAvklaringer
        Map<LocalDate, Boolean> tidligereAvklaringer = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslåttHolder().getAvklaringer().stream()
                .collect(Collectors.toMap(BostedsAvklaring::getSkjæringstidspunkt, BostedsAvklaring::erBosattITrondheim)))
            .orElse(Map.of());

        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        Set<LocalDate> fomsHvorAktivEtterlysningFinnes = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .map(e -> e.getPeriode().getFomDato())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        UUID grunnlagsreferanse = bostedsGrunnlagRepository.lagreAvklaringer(behandlingId, avklaringerPerSkjæringstidspunkt);

        // Perioder som skal ha ny etterlysning: ingen aktiv etterlysning, ELLER avklaring endret
        Set<LocalDate> fomsUtenEtterlysning = new LinkedHashSet<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            LocalDate fom = avklaring.getPeriode().getFom();
            boolean harAktivEtterlysning = fomsHvorAktivEtterlysningFinnes.contains(fom);
            boolean avklaringEndret = !avklaring.getErBosattITrondheim().equals(tidligereAvklaringer.get(fom));
            if (!harAktivEtterlysning || avklaringEndret) {
                fomsUtenEtterlysning.add(fom);
            }
        }

        if (!fomsUtenEtterlysning.isEmpty()) {
            // Avbryt eksisterende OPPRETTET-etterlysninger for perioder som skal ha ny etterlysning
            var eksisterendeOpprettede = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_BOSTED)
                .stream()
                .filter(e -> fomsUtenEtterlysning.contains(e.getPeriode().getFomDato()))
                .toList();
            eksisterendeOpprettede.forEach(Etterlysning::avbryt);
            etterlysningRepository.lagre(eksisterendeOpprettede);

            for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
                if (!fomsUtenEtterlysning.contains(avklaring.getPeriode().getFom())) {
                    continue;
                }
                var etterlysning = Etterlysning.opprettForType(
                    behandlingId,
                    grunnlagsreferanse,
                    UUID.randomUUID(),
                    DatoIntervallEntitet.fraOgMedTilOgMed(avklaring.getPeriode().getFom(), avklaring.getPeriode().getTom()),
                    EtterlysningType.UTTALELSE_BOSTED
                );
                etterlysningRepository.lagre(etterlysning);
            }

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
            .addLinje("Bostedsavklaring registrert – bruker varsles")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.rekjørSteg();
        return resultat;
    }

}
