package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dependent
public class ProgramperiodeendringEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ProgramperiodeendringEtterlysningTjeneste.class);


    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private EtterlysningRepository etterlysningRepository;

    @Inject
    public ProgramperiodeendringEtterlysningTjeneste(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                     UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                     ProsessTaskTjeneste prosessTaskTjeneste,
                                                     EtterlysningRepository etterlysningRepository) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.etterlysningRepository = etterlysningRepository;
    }

    void opprettEtterlysningerForProgramperiodeEndring(BehandlingskontrollKontekst kontekst) {
        final var gjeldendePeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(kontekst.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet perioder"));

        // Finner etterlysninger som skal opprettes og avbrytes for endring av programperiode
        final var resultatEndretProgramperiode = finnEndretProgramperiodeResultat(kontekst, gjeldendePeriodeGrunnlag);

        final var prosessTaskGruppe = new ProsessTaskGruppe();


        if (resultatEndretProgramperiode.etterlysningSomSkalAvbrytes() != null) {
            logger.info("Avbryter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalAvbrytes());
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(kontekst));
        }
        if (resultatEndretProgramperiode.etterlysningSomSkalOpprettes() != null) {
            logger.info("Oppretter etterlysning {}", resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            etterlysningRepository.lagre(resultatEndretProgramperiode.etterlysningSomSkalOpprettes());
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(kontekst, EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE));
        }
        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }


    private Resultat finnEndretProgramperiodeResultat(BehandlingskontrollKontekst kontekst, UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        final var programperioder = gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
        if (programperioder.isEmpty()) {
            throw new IllegalStateException("Kan ikke håndtere endring i ungdomsprogramperiode uten at det finnes programperioder");
        }
        return håndterTriggereForProgramperiodeendring(
            kontekst,
            gjeldendePeriodeGrunnlag
        );
    }

    private Resultat håndterTriggereForProgramperiodeendring(BehandlingskontrollKontekst kontekst,
                                                             UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        List<Etterlysning> alleEtterlysningerForAktuellType = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE);
        final var ventendeEtterlysning = finnVentendeEtterlysning(alleEtterlysningerForAktuellType);
        final var sisteMottatteEtterlysning = finnEtterlysningMedSistMottattSvar(alleEtterlysningerForAktuellType);
        if (ventendeEtterlysning.isPresent()) {
            return erstattDersomEndret(
                kontekst.getBehandlingId(),
                gjeldendePeriodeGrunnlag,
                ventendeEtterlysning.get()
            );
        } else if (sisteMottatteEtterlysning.isPresent()) {
            if (!erSisteMottatteGyldig(gjeldendePeriodeGrunnlag, sisteMottatteEtterlysning.get())) {
                return lagResultatForNyEtterlysningUtenAvbrutt(kontekst, gjeldendePeriodeGrunnlag);
            }
        } else {
            return lagResultatForNyEtterlysningUtenAvbrutt(kontekst, gjeldendePeriodeGrunnlag);
        }
        return Resultat.tom();
    }

    private static Resultat lagResultatForNyEtterlysningUtenAvbrutt(BehandlingskontrollKontekst kontekst,
                                                                    UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        final var nyEtterlysning = Etterlysning.opprettForType(
            kontekst.getBehandlingId(),
            gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
            UUID.randomUUID(),
            gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode(),
            EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE
        );
        return new Resultat(null, nyEtterlysning);
    }


    private static Optional<Etterlysning> finnEtterlysningMedSistMottattSvar(List<Etterlysning> aktuelleEtterlysningerForEndring) {
        return aktuelleEtterlysningerForEndring.stream()
            .filter(it -> it.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR))
            .max(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt));
    }

    private static Optional<Etterlysning> finnVentendeEtterlysning(List<Etterlysning> aktuelleEtterlysningerForEndring) {
        final var ventendeEtterlysninger = aktuelleEtterlysningerForEndring.stream()
            .filter(it -> it.getStatus().equals(EtterlysningStatus.OPPRETTET) || it.getStatus().equals(EtterlysningStatus.VENTER)).toList();


        if (ventendeEtterlysninger.size() > 1) {
            throw new IllegalStateException("Forventet å finne maksimalt en etterlysning fant " + ventendeEtterlysninger.size());
        }
        return ventendeEtterlysninger.isEmpty() ? Optional.empty() : Optional.of(ventendeEtterlysninger.getFirst());
    }

    private Resultat erstattDersomEndret(Long behandlingId,
                                         UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                         Etterlysning ventendeEtterlysning) {
        final var endringTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(ventendeEtterlysning.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());
        if (!endringTidslinje.isEmpty()) {
            final var skalOpprettes = Etterlysning.opprettForType(
                behandlingId,
                gjeldendePeriodeGrunnlag.getGrunnlagsreferanse(),
                UUID.randomUUID(),
                gjeldendePeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode(),
                EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE
            );
            ventendeEtterlysning.avbryt();
            return new Resultat(
                ventendeEtterlysning,
                skalOpprettes
            );
        }
        return Resultat.tom();
    }

    private boolean erSisteMottatteGyldig(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag,
                                          Etterlysning sisteMottatte) {
        final var endringTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(sisteMottatte.getGrunnlagsreferanse(), gjeldendePeriodeGrunnlag.getGrunnlagsreferanse());
        return endringTidslinje.isEmpty();
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


    record Resultat(Etterlysning etterlysningSomSkalAvbrytes,
                    Etterlysning etterlysningSomSkalOpprettes) {
        static Resultat tom() {
            return new Resultat(null, null);
        }
    }

}
