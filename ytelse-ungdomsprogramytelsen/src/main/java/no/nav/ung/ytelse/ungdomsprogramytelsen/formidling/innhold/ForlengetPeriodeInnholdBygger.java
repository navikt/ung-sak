package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.ForlengetPeriodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Dependent
public class ForlengetPeriodeInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(ForlengetPeriodeInnholdBygger.class);

    private final ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public ForlengetPeriodeInnholdBygger(ProsessTriggereRepository prosessTriggereRepository) {
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        // Bruk trigger-perioden direkte. Trigger-perioden settes av
        // UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder og representerer den nye
        // delta-perioden (justert til riktig virkedag) som behandlingen forlenger ungdomsprogrammet med.
        List<Trigger> forlengetTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId())
            .map(ProsessTriggere::getTriggere)
            .stream()
            .flatMap(Collection::stream)
            .filter(t -> BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM.equals(t.getÅrsak()))
            .toList();

        if (forlengetTriggere.isEmpty()) {
            throw new IllegalStateException(
                "Forventet prosesstrigger med årsak RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM på behandling="
                    + behandling.getId() + " ved bygging av brev for forlenget periode");
        }

        if (forlengetTriggere.size() > 1) {
            // Skal i utgangspunktet ikke skje – idempotens-sjekken i hendelsemottak hindrer normalt
            // at det opprettes flere triggere for samme forlengelse. Logger for å fange opp
            // eventuelle feil i hendelsemottak, og velger trigger med tidligst fom-dato deterministisk.
            LOG.warn("Flere triggere ({}) med årsak RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM funnet på behandling={}. Velger trigger med tidligst fom-dato.",
                forlengetTriggere.size(), behandling.getId());
        }

        LocalDate forlengetPeriodeFraOgMedDato = forlengetTriggere.stream()
            .map(Trigger::getPeriode)
            .map(p -> p.getFomDato())
            .min(Comparator.naturalOrder())
            .orElseThrow();

        return new TemplateInnholdResultat(TemplateType.FORLENGET_PERIODE,
            new ForlengetPeriodeDto(forlengetPeriodeFraOgMedDato));
    }
}




