package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
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

    public void oppdaterEtterlysninger(Behandling behandling,
                                        EtterlysningType etterlysningType,
                                        Collection<VilkårPeriodeAvklaring> tidligereForeslåtte,
                                        Collection<VilkårPeriodeAvklaring> nyeForeslåtte) {

        long behandlingId = behandling.getId();

        var etterlysningerSomVenterSvar = etterlysningRepository
            .hentEtterlysningerSomVenterPåSvar(behandlingId).stream()
            .filter(e -> e.getType() == etterlysningType)
            .toList();

        Map<VilkårsavklaringInnhold, UUID> tidligereAvklaringer = tidligereForeslåtte.stream()
            .collect(Collectors.toMap(
                VilkårsavklaringInnhold::fra,
                VilkårPeriodeAvklaring::getReferanse
            ));

        Map<VilkårsavklaringInnhold, UUID> avklaringerSomSkalVarsles = nyeForeslåtte.stream()
            .filter(VilkårPeriodeAvklaring::skalSendeVarsel)
            .collect(Collectors.toMap(
                VilkårsavklaringInnhold::fra,
                VilkårPeriodeAvklaring::getReferanse)
            );

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
                avklaring.getKey().periode(),
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

    /**
     * Innholdssammenligning uten {@code referanse}, {@code vurdertAv} og {@code vurdertTidspunkt} — to
     * avklaringer med samme innhold skal ikke trigge en ny etterlysning, selv om de er separate instanser.
     */
    private record VilkårsavklaringInnhold(DatoIntervallEntitet periode,
                                            String ikkeOppfyltÅrsakKode,
                                            String begrunnelse,
                                            boolean skalSendeVarsel,
                                            String fritekstTilVarsel,
                                            String begrunnelseIkkeVarsel,
                                            Avklaringtype avklaringtype) {

        static VilkårsavklaringInnhold fra(VilkårPeriodeAvklaring avklaring) {
            return new VilkårsavklaringInnhold(
                avklaring.getPeriode(),
                avklaring.getIkkeOppfyltÅrsakKode(),
                avklaring.getBegrunnelse(),
                avklaring.skalSendeVarsel(),
                avklaring.getFritekstTilVarsel(),
                avklaring.getBegrunnelseIkkeVarsel(),
                avklaring.getAvklaringtype()
            );
        }
    }
}
