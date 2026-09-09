package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Felles etterlysningslogikk for vilkårsavklaringer, uavhengig av hvilket vilkår det gjelder.
 * Avbryter etterlysninger som venter svar og ikke lenger har uendret innhold, oppretter nye etterlysninger for
 * avklaringer med {@code skalSendeVarsel}.
 */
@ApplicationScoped
public class VilkårsavklaringEtterlysningTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VilkårsavklaringEtterlysningTjeneste.class);

    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    VilkårsavklaringEtterlysningTjeneste() {
        // CDI
    }

    @Inject
    public VilkårsavklaringEtterlysningTjeneste(EtterlysningRepository etterlysningRepository,
                                                 ProsessTaskTjeneste prosessTaskTjeneste) {
        this.etterlysningRepository = etterlysningRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    /**
     * Avklaringsinnholdet er mappet til en vilkårsspesifikk {@link VilkårsavklaringInnhold}-implementasjon
     * (skjuler den konkrete entitetsrepresentasjonen for denne tjenesten), sammen med referansen den ble/skal
     * lagres med.
     */
    public void oppdaterEtterlysninger(Behandling behandling,
                                        EtterlysningType etterlysningType,
                                        Map<? extends VilkårsavklaringInnhold, UUID> tidligereForeslåtte,
                                        Map<? extends VilkårsavklaringInnhold, UUID> nyeForeslåtte) {

        long behandlingId = behandling.getId();

        var etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == etterlysningType)
            .toList();

        Map<VilkårsavklaringInnhold, UUID> tidligereAvklaringer = new HashMap<>(tidligereForeslåtte);

        Map<VilkårsavklaringInnhold, UUID> avklaringerSomSkalVarsles = nyeForeslåtte.entrySet().stream()
            .filter(entry -> entry.getKey().skalSendeVarsel())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var referanserForUendretInnhold = tidligereAvklaringer.entrySet().stream()
            .filter(entry -> avklaringerSomSkalVarsles.containsKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        var etterlysningerSomSkalAvbrytes = etterlysningerSomVenterSvar.stream()
            .filter(etterlysning -> !referanserForUendretInnhold.contains(etterlysning.getGrunnlagsreferanse()))
            .peek(Etterlysning::setSkalAvbrytes)
            .toList();
        etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);

        // Beholder kun nye avklaringer og avklaringer med endret innhold
        avklaringerSomSkalVarsles.keySet().removeAll(tidligereAvklaringer.keySet());
        var nyeEtterlysninger = avklaringerSomSkalVarsles.entrySet().stream().map(avklaring ->
            Etterlysning.opprettForType(
                behandlingId,
                avklaring.getValue(),
                UUID.randomUUID(),
                avklaring.getKey().hentPeriodeSomDatoIntervallEntitet(),
                etterlysningType
            )).toList();
        etterlysningRepository.lagre(nyeEtterlysninger);

        var skalAvbryte = etterlysningerSomSkalAvbrytes.stream().anyMatch(it -> it.getStatus() == EtterlysningStatus.SKAL_AVBRYTES);
        if (skalAvbryte) {
            log.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            var task = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            prosessTaskTjeneste.lagre(task);
        }

        if (!avklaringerSomSkalVarsles.isEmpty()) {
            log.info("Oppretter etterlysninger {}", avklaringerSomSkalVarsles);
            var task = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
            task.setBehandling(behandling.getFagsakId(), behandlingId);
            task.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, etterlysningType.getKode());
            prosessTaskTjeneste.lagre(task);
        }
    }
}
