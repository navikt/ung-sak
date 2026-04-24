package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
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

        // Les eksisterende foreslåtte avklaringer per fomDato BEFORE lagreAvklaringer
        Map<LocalDate, Boolean> tidligereAvklaringer = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getForeslåttHolder().getAvklaringer().stream()
                .collect(Collectors.toMap(BostedsAvklaring::getFomDato, BostedsAvklaring::erBosattITrondheim)))
            .orElse(Map.of());

        // Bygg nye avklaringer med splitt basert på vurdering
        Map<LocalDate, Boolean> nyeAvklaringer = new LinkedHashMap<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            nyeAvklaringer.putAll(BostedAvklaringUtil.splittAvklaring(avklaring.getPeriode().getFom(), avklaring.getVurdering()));
        }

        // Hent eksisterende aktive etterlysninger (OPPRETTET/VENTER) per fom
        Set<LocalDate> fomsHvorAktivEtterlysningFinnes = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == EtterlysningType.UTTALELSE_BOSTED)
            .map(e -> e.getPeriode().getFomDato())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        UUID grunnlagsreferanse = bostedsGrunnlagRepository.lagreAvklaringer(behandlingId, nyeAvklaringer);

        // Hent søknadens bosted fra grunnlaget (for sammenligning med saksbehandlerens vurdering)
        Map<LocalDate, Boolean> søknadErBosattPerFom = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(g -> g.getSøknadHolder() != null
                ? g.getSøknadHolder().getAvklaringer().stream()
                    .collect(Collectors.toMap(BostedsAvklaring::getFomDato, BostedsAvklaring::erBosattITrondheim))
                : Map.<LocalDate, Boolean>of())
            .orElse(Map.of());

        // Perioder som skal ha ny etterlysning: ingen aktiv etterlysning OG søknad stemmer ikke, ELLER avklaring endret
        Set<LocalDate> fomsUtenEtterlysning = new LinkedHashSet<>();
        for (BostedAvklaringPeriodeDto avklaring : dto.getAvklaringer()) {
            LocalDate periodesFom = avklaring.getPeriode().getFom();
            LocalDate periodesTom = avklaring.getPeriode().getTom();
            boolean harAktivEtterlysning = fomsHvorAktivEtterlysningFinnes.contains(periodesFom);

            Map<LocalDate, Boolean> nyeForPeriode = BostedAvklaringUtil.splittAvklaring(avklaring.getPeriode().getFom(), avklaring.getVurdering());
            Map<LocalDate, Boolean> gamleForPeriode = tidligereAvklaringer.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(periodesFom) && !e.getKey().isAfter(periodesTom))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            boolean avklaringEndret = !nyeForPeriode.equals(gamleForPeriode);

            Boolean søknadErBosatt = søknadErBosattPerFom.get(periodesFom);
            boolean søknadStemmerOverens = søknadErBosatt != null && nyeForPeriode.equals(Map.of(periodesFom, søknadErBosatt));
            if ((!harAktivEtterlysning && !søknadStemmerOverens) || avklaringEndret) {
                fomsUtenEtterlysning.add(periodesFom);
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
            .addLinje(fomsUtenEtterlysning.isEmpty()
                ? "Bostedsavklaring registrert"
                : "Bostedsavklaring registrert – bruker varsles")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        var resultat = OppdateringResultat.nyttResultat();
        resultat.setSteg(BehandlingStegType.VURDER_BOSTED);
        resultat.rekjørSteg();
        return resultat;
    }

}
