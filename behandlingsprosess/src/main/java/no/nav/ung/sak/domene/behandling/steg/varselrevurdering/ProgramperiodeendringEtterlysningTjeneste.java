package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_BEGYNNELSE;
import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class ProgramperiodeendringEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ProgramperiodeendringEtterlysningTjeneste.class);


    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public ProgramperiodeendringEtterlysningTjeneste(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                     UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                                     EtterlysningRepository etterlysningRepository,
                                                     ProsessTriggereRepository prosessTriggereRepository) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    void opprettEtterlysningerForProgramperiodeEndring(BehandlingskontrollKontekst kontekst) {
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        final var relevanteTriggerForPeriodeendring = finnRelevanteTriggere(kontekst.getBehandlingId());
        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        // Finner etterlysninger som skal opprettes og avbrytes for endring av startdato
        final var delResultatEndretStartdato = finnEndretStartdatoDelresultat(kontekst, relevanteTriggerForPeriodeendring, gjeldendePeriodeGrunnlag);
        etterlysningerSomSkalOpprettes.addAll(delResultatEndretStartdato.etterlysningerSomSkalOpprettes());
        etterlysningerSomSkalAvbrytes.addAll(delResultatEndretStartdato.etterlysningerSomSkalAvbrytes());

        // Finner etterlysninger som skal opprettes og avbrytes for endring av sluttdato
        final var delresultatEndretSluttdato = finnEndretSluttdatoDelresultat(kontekst, relevanteTriggerForPeriodeendring, gjeldendePeriodeGrunnlag);
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
            final var typer = etterlysningerSomSkalOpprettes.stream().map(Etterlysning::getType).collect(Collectors.toSet());
            typer.forEach(type -> prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(kontekst, type)));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }

    private Delresultat finnEndretSluttdatoDelresultat(BehandlingskontrollKontekst kontekst, List<Trigger> relevanteTriggerForEtterlysning, UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();
        // Finner prosesstriggere for opphør der opphørsdatoen i den opprinnelige hendelsen fortsatt er gjeldende
        final var aktuelleTriggereForGjeldendeSluttdatoer = finnRelevanteTriggereForSluttdato(programperioder, relevanteTriggerForEtterlysning);
        return håndterTriggereForProgramperiodeendring(kontekst,
            gjeldendePeriodeGrunnlag,
            aktuelleTriggereForGjeldendeSluttdatoer,
            (trigger -> finnAktuellPeriodeForEndretSluttdato(trigger, programperioder)),
            EtterlysningType.UTTALELSE_ENDRET_SLUTTDATO);
    }

    private Delresultat finnEndretStartdatoDelresultat(BehandlingskontrollKontekst kontekst, List<Trigger> relevanteTriggerForEtterlysning, UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();
        // Finner prosesstriggere for endring i startdato der startdatoen i den opprinnelige hendelsen fortsatt er gjeldende
        final var aktuelleTriggereForGjeldendeStartdatoer = finnRelevanteTriggereForStartdato(programperioder, relevanteTriggerForEtterlysning);
        final AktuellPeriodeUtleder aktuellPeriodeUtlederForEndretStartdato = trigger -> finnAktuellPeriodeForEndretStartdato(trigger, programperioder);
        return håndterTriggereForProgramperiodeendring(
            kontekst,
            gjeldendePeriodeGrunnlag,
            aktuelleTriggereForGjeldendeStartdatoer,
            aktuellPeriodeUtlederForEndretStartdato,
            EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
    }

    private List<Trigger> finnRelevanteTriggere(Long behandlingId) {
        return prosessTriggereRepository.hentGrunnlag(behandlingId)
            .stream()
            .map(ProsessTriggere::getTriggere)
            .flatMap(Collection::stream)
            .filter(trigger ->
                trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM ||
                    trigger.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
            )
            .toList();
    }

    private Delresultat håndterTriggereForProgramperiodeendring(BehandlingskontrollKontekst kontekst,
                                                                UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                                Set<Trigger> aktuelleTriggere,
                                                                AktuellPeriodeUtleder aktuellPeriodeUtleder,
                                                                EtterlysningType aktuellEtterlysningsType) {
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        List<Etterlysning> alleEtterlysningerForAktuellType = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), aktuellEtterlysningsType);
        for (var triggerForEndring : aktuelleTriggere) {
            // Perioder der vi ser etter etterlysninger som gjelder samme endring
            // ved endringer i flere programperioder i samme behandling skal det vere lov å ha flere etterlysninger for ulike periodeendringer av samme type gitt at disse gjelder for ulike deler av perioden
            final var periodeForAktuelleEtterlysninger = aktuellPeriodeUtleder.utledAktuellPeriode(triggerForEndring);
            final var relevanteEtterlysninger = finnRelevanteEtterlysninger(alleEtterlysningerForAktuellType, periodeForAktuelleEtterlysninger);
            final var ventendeEtterlysninger = finnVentendeEtterlysning(relevanteEtterlysninger, periodeForAktuelleEtterlysninger);
            final var sisteMottatteEtterlysning = finnEtterlysningMedSistMottattSvar(relevanteEtterlysninger);
            if (ventendeEtterlysninger.isPresent()) {
                final var erstattResultat = erstattDersomEndret(
                    kontekst.getBehandlingId(), gjeldendePeriodeGrunnlag, aktuellEtterlysningsType, triggerForEndring,
                    ventendeEtterlysninger.get()
                );

                erstattResultat.ifPresent(d -> {
                        etterlysningerSomSkalAvbrytes.add(d.skalAvbrytes);
                        etterlysningerSomSkalOpprettes.add(d.skalOpprettes);
                    }
                );

            } else if (sisteMottatteEtterlysning.isPresent()) {
                final var erstattResultat = erstattDersomEndret(
                    kontekst.getBehandlingId(), gjeldendePeriodeGrunnlag, aktuellEtterlysningsType, triggerForEndring,
                    sisteMottatteEtterlysning.get()
                );

                erstattResultat.ifPresent(d -> {
                        etterlysningerSomSkalAvbrytes.add(d.skalAvbrytes());
                        etterlysningerSomSkalOpprettes.add(d.skalOpprettes());
                    }
                );
            } else {
                etterlysningerSomSkalOpprettes.add(Etterlysning.opprettForType(
                    kontekst.getBehandlingId(),
                    gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                    UUID.randomUUID(),
                    triggerForEndring.getPeriode(),
                    aktuellEtterlysningsType
                ));
            }
            return new Delresultat(etterlysningerSomSkalAvbrytes, etterlysningerSomSkalOpprettes);
        }
        return new Delresultat(etterlysningerSomSkalAvbrytes, etterlysningerSomSkalOpprettes);
    }

    private static List<Etterlysning> finnRelevanteEtterlysninger(List<Etterlysning> relevanteEtterlysninger, DatoIntervallEntitet periodeForAktuelleEtterlysninger) {
        return relevanteEtterlysninger.stream().filter(e -> periodeForAktuelleEtterlysninger.inkluderer(e.getPeriode().getFomDato())).toList();
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
        // Startdato oppgitt i den opprinnelige hendelsen
        final var startdato = triggerForEndring.getPeriode().getFomDato();

        // Programperioden som er knyttet til den opprinnelige hendelsen
        final var aktuellProgramperiode = programperioder.stream()
            .filter(it -> it.getPeriode().getFomDato().equals(startdato))
            .findFirst().orElseThrow(() -> new IllegalStateException("Forventer å finne programperiode for startdato"));

        // Programperioden i forkant (dersom den eksisterer)
        final var programperiodeFør = programperioder.stream()
            .filter(it -> it.getPeriode().getTomDato().isBefore(startdato))
            .max(Comparator.comparing(p -> p.getPeriode().getTomDato()));

        // Alle endringer i startdato siden slutten av forrige periode og til slutten av den aktuelle perioden regnes som aktuelle
        final var aktuelleEtterlysningerFom = programperiodeFør.map(UngdomsprogramPeriode::getPeriode).map(DatoIntervallEntitet::getTomDato).orElse(TIDENES_BEGYNNELSE);
        final var aktuelleEtterlysningerTom = aktuellProgramperiode.getPeriode().getTomDato();
        return DatoIntervallEntitet.fraOgMedTilOgMed(aktuelleEtterlysningerFom, aktuelleEtterlysningerTom);
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

    private Optional<ErstattEtterlysningResultat> erstattDersomEndret(Long behandlingId,
                                                                      UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                                                      EtterlysningType etterlysningType,
                                                                      Trigger triggerForEndring,
                                                                      Etterlysning ventendeEtterlysning) {
        final var endringTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());

        if (!endringTidslinje.isEmpty()) {
            final var skalOpprettes = Etterlysning.opprettForType(
                behandlingId,
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

    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(BehandlingskontrollKontekst kontekst, EtterlysningType type) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, type.getKode());
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
