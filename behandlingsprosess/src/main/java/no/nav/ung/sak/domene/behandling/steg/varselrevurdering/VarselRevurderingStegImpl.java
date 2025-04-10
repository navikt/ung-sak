package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VARSEL_REVURDERING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;
import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_BEGYNNELSE;
import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@BehandlingStegRef(value = VARSEL_REVURDERING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VarselRevurderingStegImpl implements VarselRevurderingSteg {
    private static final Logger logger = LoggerFactory.getLogger(VarselRevurderingStegImpl.class);

    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private final Duration ventePeriode;

    @Inject
    public VarselRevurderingStegImpl(BehandlingRepository behandlingRepository,
                                     UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                     UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                     ProsessTaskTjeneste prosessTaskTjeneste, EtterlysningRepository etterlysningRepository, ProsessTriggereRepository prosessTriggereRepository,
                                     @KonfigVerdi(value = "REVURDERING_ENDRET_PERIODE_VENTEFRIST", defaultVerdi = "P14D") String ventePeriode) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        List<BehandlingÅrsakType> behandlingÅrsakerTyper = behandling.getBehandlingÅrsakerTyper();

        boolean skalOppretteEtterlysning = behandlingÅrsakerTyper.stream()
            .anyMatch(årsak ->
                BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM == årsak ||
                    BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM == årsak
            );

        if (skalOppretteEtterlysning) {
            opprettTaskForEtterlysning(kontekst);
        }

        final var endretProgramperiodeEtterlysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO, EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        final var nyopprettetEtterlysning = endretProgramperiodeEtterlysninger.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET)).findFirst();
        if (nyopprettetEtterlysning.isPresent()) {
            final var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AUTO_SATT_PÅ_VENT_REVURDERING,
                nyopprettetEtterlysning.get().getType().equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO) ? Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM : Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM,
                LocalDateTime.now().plus(ventePeriode));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
        }


        final var etterlysningSomVentesPå = endretProgramperiodeEtterlysninger
            .stream().filter(it -> it.getStatus().equals(EtterlysningStatus.VENTER))
            .toList();
        if (!etterlysningSomVentesPå.isEmpty()) {
            final var lengsteFristEtterlysning = etterlysningSomVentesPå.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.VENTER))
                .max(Comparator.comparing(Etterlysning::getFrist)).orElseThrow(() -> new IllegalStateException("Forventer å finne en etterlysning på vent"));
            final var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AUTO_SATT_PÅ_VENT_REVURDERING,
                lengsteFristEtterlysning.getType().equals(EtterlysningType.UTTALELSE_ENDRET_STARTDATO) ? Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM : Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM,
                lengsteFristEtterlysning.getFrist());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


    private void opprettTaskForEtterlysning(BehandlingskontrollKontekst kontekst) {
        List<Trigger> relevanteTriggerForEtterlysning = prosessTriggereRepository.hentGrunnlag(kontekst.getBehandlingId())
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .filter(trigger ->
                trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM ||
                    trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
            )
            .toList();


        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();

        final var aktuelleTriggereForGjeldendeStartdatoer = finnRelevanteTriggereForStartdato(programperioder, relevanteTriggerForEtterlysning);

        final var delResultatEndretStartdato = håndterTriggereForProgramperiodeendring(
            kontekst,
            gjeldendePeriodeGrunnlag,
            aktuelleTriggereForGjeldendeStartdatoer,
            (trigger -> finnAktuellPeriodeForEndretStartdato(trigger, programperioder)),
            EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        etterlysningerSomSkalOpprettes.addAll(delResultatEndretStartdato.etterlysningerSomSkalOpprettes());
        etterlysningerSomSkalAvbrytes.addAll(delResultatEndretStartdato.etterlysningerSomSkalAvbrytes());

        final var aktuelleTriggereForGjeldendeSluttdatoer = finnRelevanteTriggereForSluttdato(programperioder, relevanteTriggerForEtterlysning);
        final var delresultatEndretSluttdato = håndterTriggereForProgramperiodeendring(kontekst,
            gjeldendePeriodeGrunnlag,
            aktuelleTriggereForGjeldendeSluttdatoer,
            (trigger -> finnAktuellPeriodeForEndretSluttdato(trigger, programperioder)),
            EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
        etterlysningerSomSkalOpprettes.addAll(delresultatEndretSluttdato.etterlysningerSomSkalOpprettes());
        etterlysningerSomSkalAvbrytes.addAll(delresultatEndretSluttdato.etterlysningerSomSkalAvbrytes());


        final var prosessTaskGruppe = new ProsessTaskGruppe();


        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            logger.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(kontekst));
        }
        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            logger.info("Oppretter etterlysninger {}", etterlysningerSomSkalOpprettes);
            etterlysningRepository.lagre(etterlysningerSomSkalOpprettes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(kontekst));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }

    private Delresultat håndterTriggereForProgramperiodeendring(BehandlingskontrollKontekst kontekst,
                                                                UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                                Set<Trigger> aktuelleTriggereForGjeldendeStartdatoer,
                                                                AktuellPeriodeUtleder utleder, EtterlysningType etterlysningType) {
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        List<Etterlysning> relevanteEtterlysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), etterlysningType);
        for (var triggerForEndring : aktuelleTriggereForGjeldendeStartdatoer) {
            final var periodeForAktuelleEtterlysninger = utleder.utledAktuellPeriode(triggerForEndring);
            final var aktuelleEtterlysningerForEndring = relevanteEtterlysninger.stream().filter(e -> periodeForAktuelleEtterlysninger.inkluderer(e.getPeriode().getFomDato())).toList();
            final var ventendeEtterlysninger = finnVentendeEtterlysning(aktuelleEtterlysningerForEndring, periodeForAktuelleEtterlysninger);
            final var sisteMottatteEtterlysning = finnEtterlysningMedSistMottattSvar(aktuelleEtterlysningerForEndring);
            if (ventendeEtterlysninger.isPresent()) {
                final var erstattResultat = erstattDersomEndret(kontekst,
                    triggerForEndring,
                    ventendeEtterlysninger.get(),
                    gjeldendePeriodeGrunnlag,
                    etterlysningType);

                erstattResultat.ifPresent(d -> {
                        etterlysningerSomSkalAvbrytes.add(d.skalAvbrytes);
                        etterlysningerSomSkalOpprettes.add(d.skalOpprettes);
                    }
                );

            } else if (sisteMottatteEtterlysning.isPresent()) {
                final var erstattResultat = erstattDersomEndret(kontekst,
                    triggerForEndring,
                    sisteMottatteEtterlysning.get(),
                    gjeldendePeriodeGrunnlag,
                    etterlysningType);

                erstattResultat.ifPresent(d -> {
                        etterlysningerSomSkalAvbrytes.add(d.skalAvbrytes);
                        etterlysningerSomSkalOpprettes.add(d.skalOpprettes);
                    }
                );
            } else {
                etterlysningerSomSkalOpprettes.add(Etterlysning.opprettForType(
                    kontekst.getBehandlingId(),
                    gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                    UUID.randomUUID(),
                    triggerForEndring.getPeriode(),
                    etterlysningType
                ));
            }

            return new Delresultat(etterlysningerSomSkalAvbrytes, etterlysningerSomSkalOpprettes);


        }

        return new Delresultat(etterlysningerSomSkalAvbrytes, etterlysningerSomSkalOpprettes);
    }

    private static Set<Trigger> finnRelevanteTriggereForStartdato(Set<UngdomsprogramPeriode> programperioder, List<Trigger> relevanteTriggerForEtterlysning) {
        final var gjeldendeStartdatoer = programperioder.stream().
            map(UngdomsprogramPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());
        return finnAktuelleTriggereForDato(relevanteTriggerForEtterlysning, gjeldendeStartdatoer, BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
    }


    private static Set<Trigger> finnRelevanteTriggereForSluttdato(Set<UngdomsprogramPeriode> programperioder, List<Trigger> relevanteTriggerForEtterlysning) {
        final var gjeldendeSluttdatoer = programperioder.stream()
            .map(UngdomsprogramPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .collect(Collectors.toSet());
        return finnAktuelleTriggereForDato(relevanteTriggerForEtterlysning, gjeldendeSluttdatoer, BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
    }


    private static Set<Trigger> finnAktuelleTriggereForDato(List<Trigger> relevanteTriggerForEtterlysning, Set<LocalDate> aktuelleDatoer, BehandlingÅrsakType behandlingÅrsakType) {
        return relevanteTriggerForEtterlysning.stream()
            .filter(it -> it.getÅrsak().equals(behandlingÅrsakType))
            .filter(it -> aktuelleDatoer.contains(it.getPeriode().getFomDato()))
            .collect(Collectors.toSet());
    }

    private static DatoIntervallEntitet finnAktuellPeriodeForEndretStartdato(Trigger triggerForEndring, Set<UngdomsprogramPeriode> programperioder) {
        final var startdato = triggerForEndring.getPeriode().getFomDato();

        final var aktuellProgramperiode = programperioder.stream()
            .filter(it -> it.getPeriode().getFomDato().equals(startdato))
            .findFirst().orElseThrow(() -> new IllegalStateException("Forventer å finne programperiode for startdato"));

        final var programperiodeFør = programperioder.stream()
            .filter(it -> it.getPeriode().getTomDato().isBefore(startdato))
            .max(Comparator.comparing(p -> p.getPeriode().getTomDato()));


        final var aktuelleEtterlysningerFom = programperiodeFør.map(UngdomsprogramPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato).orElse(TIDENES_BEGYNNELSE);
        final var aktuelleEtterlysningerTom = aktuellProgramperiode.getPeriode().getTomDato();

        final var periodeForAktuelleEtterlysninger = DatoIntervallEntitet.fraOgMedTilOgMed(aktuelleEtterlysningerFom, aktuelleEtterlysningerTom);
        return periodeForAktuelleEtterlysninger;
    }


    private static DatoIntervallEntitet finnAktuellPeriodeForEndretSluttdato(Trigger triggerForEndring, Set<UngdomsprogramPeriode> programperioder) {
        final var sluttdato = triggerForEndring.getPeriode().getFomDato();

        final var aktuellProgramperiode = programperioder.stream()
            .filter(it -> it.getPeriode().getTomDato().equals(sluttdato))
            .findFirst().orElseThrow(() -> new IllegalStateException("Forventer å finne programperiode for startdato"));

        final var programperiodeEtter = programperioder.stream()
            .filter(it -> it.getPeriode().getFomDato().isAfter(sluttdato))
            .min(Comparator.comparing(p -> p.getPeriode().getFomDato()));

        final var aktuelleEtterlysningerTom = programperiodeEtter.map(UngdomsprogramPeriode::getPeriode).map(DatoIntervallEntitet::getFomDato).orElse(TIDENES_ENDE);
        final var aktuelleEtterlysningerFom = aktuellProgramperiode.getPeriode().getFomDato();
        return DatoIntervallEntitet.fraOgMedTilOgMed(aktuelleEtterlysningerFom, aktuelleEtterlysningerTom);
    }

    private static Optional<Etterlysning> finnEtterlysningMedSistMottattSvar(List<Etterlysning> aktuelleEtterlysningerForEndring) {
        return aktuelleEtterlysningerForEndring.stream()
            .filter(it -> it.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR))
            .max(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt));
    }

    private static Optional<Etterlysning> finnVentendeEtterlysning(List<Etterlysning> aktuelleEtterlysningerForEndring, DatoIntervallEntitet periodeForAktuelleEtterlysninger) {
        final var ventendeEtterlysninger = aktuelleEtterlysningerForEndring.stream()
            .filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET) || it.getStatus().equals(EtterlysningStatus.VENTER)).toList();


        if (ventendeEtterlysninger.size() > 1) {
            throw new IllegalStateException("Forventet å finne maksimalt en etterlysning for aktuell periode " + periodeForAktuelleEtterlysninger + ", fant " + ventendeEtterlysninger.size());
        }
        return ventendeEtterlysninger.isEmpty() ? Optional.empty() : Optional.of(ventendeEtterlysninger.getFirst());
    }

    private Optional<ErstattEtterlysningResultat> erstattDersomEndret(BehandlingskontrollKontekst kontekst,
                                                                      Trigger triggerForEndring,
                                                                      Etterlysning ventendeEtterlysning,
                                                                      UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                                      EtterlysningType etterlysningType) {
        final var endringTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());

        if (!endringTidslinje.isEmpty()) {
            final var skalOpprettes = Etterlysning.opprettForType(
                kontekst.getBehandlingId(),
                gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                UUID.randomUUID(),
                triggerForEndring.getPeriode(),
                etterlysningType
            );
            return Optional.of(
                new ErstattEtterlysningResultat(
                    ventendeEtterlysning,
                    skalOpprettes
                ));
        }
        return Optional.empty();
    }

    private ProsessTaskData lagTaskForAvbrytelseAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.getKode());
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    @FunctionalInterface
    interface AktuellPeriodeUtleder {
        DatoIntervallEntitet utledAktuellPeriode(Trigger trigger);
    }


    record Delresultat(List<Etterlysning> etterlysningerSomSkalAvbrytes,
                       List<Etterlysning> etterlysningerSomSkalOpprettes) {
    }

    record ErstattEtterlysningResultat(Etterlysning skalAvbrytes, Etterlysning skalOpprettes) {
    }

}
