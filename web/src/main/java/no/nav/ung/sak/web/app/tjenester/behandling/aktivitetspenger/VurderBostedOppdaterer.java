package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedAvklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderBostedDto;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBostedDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBostedOppdaterer implements AksjonspunktOppdaterer<VurderBostedDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private EtterlysningRepository etterlysningRepository;
    private UttalelseRepository uttalelseRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    VurderBostedOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBostedOppdaterer(BehandlingRepository behandlingRepository,
                                  HistorikkinnslagRepository historikkinnslagRepository,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                  EtterlysningRepository etterlysningRepository,
                                  UttalelseRepository uttalelseRepository,
                                  ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.uttalelseRepository = uttalelseRepository;
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

        UUID grunnlagsreferanse = bostedsGrunnlagRepository.lagreAvklaringer(behandlingId, avklaringerPerSkjæringstidspunkt);

        // Finn perioder som har mottatt uttalelse – disse fastsettes umiddelbart (ingen ny etterlysning)
        Set<LocalDate> fomsUtenEtterlysning = finnFomerSomMåSendeEtterlysning(behandlingId, dto.getAvklaringer());
        Set<LocalDate> fomsMedUttalelse = new LinkedHashSet<>(avklaringerPerSkjæringstidspunkt.keySet());
        fomsMedUttalelse.removeAll(fomsUtenEtterlysning);

        if (!fomsMedUttalelse.isEmpty()) {
            // Re-vurdering etter brukerens uttalelse: fastsett umiddelbart
            bostedsGrunnlagRepository.fastsettForeslåtteAvklaringer(behandlingId, fomsMedUttalelse);
        }

        if (!fomsUtenEtterlysning.isEmpty()) {
            // Avbryt eksisterende OPPRETTET-etterlysninger og opprett nye
            var eksisterendeOpprettede = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_BOSTED);
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
            .addLinje(fomsUtenEtterlysning.isEmpty()
                ? "Bostedsavklaring oppdatert etter brukerens uttalelse"
                : "Bostedsavklaring registrert – bruker varsles")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.rekjørSteg();
        return resultat;
    }

    /**
     * Returnerer fom-datoer for perioder som IKKE har MOTTATT_SVAR-etterlysning med uttalelse,
     * og som dermed skal varsles på nytt via ny etterlysning.
     */
    private Set<LocalDate> finnFomerSomMåSendeEtterlysning(long behandlingId, List<BostedAvklaringPeriodeDto> avklaringer) {
        var mottattSvarEtterlysninger = etterlysningRepository.hentEtterlysningerMedSisteFørst(behandlingId, EtterlysningType.UTTALELSE_BOSTED)
            .stream()
            .filter(e -> e.getStatus() == EtterlysningStatus.MOTTATT_SVAR)
            .toList();

        var uttalelser = uttalelseRepository.hentUttalelser(behandlingId, EndringType.AVKLAR_BOSTED);

        Set<LocalDate> fomsWithUttalelse = new LinkedHashSet<>();
        for (Etterlysning etterlysning : mottattSvarEtterlysninger) {
            boolean harUttalelse = uttalelser.stream()
                .filter(u -> u.getPeriode().equals(etterlysning.getPeriode())
                    && u.getGrunnlagsreferanse().equals(etterlysning.getGrunnlagsreferanse()))
                .anyMatch(UttalelseV2::harUttalelse);
            if (harUttalelse) {
                fomsWithUttalelse.add(etterlysning.getPeriode().getFomDato());
            }
        }

        Set<LocalDate> fomsUtenEtterlysning = new LinkedHashSet<>();
        for (BostedAvklaringPeriodeDto avklaring : avklaringer) {
            if (!fomsWithUttalelse.contains(avklaring.getPeriode().getFom())) {
                fomsUtenEtterlysning.add(avklaring.getPeriode().getFom());
            }
        }
        return fomsUtenEtterlysning;
    }

}
