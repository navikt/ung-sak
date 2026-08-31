package no.nav.ung.sak.etterlysning.bistand;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Oppretter oppgave til deltaker for etterlysning av uttalelse om bistandsavklaring.
 * <p>
 * TODO(fase 1 plassholder): Faktisk oppgave-sending til ung-brukerdialog-api er IKKE koblet opp. Kontrakten
 * {@code no.nav.ung.brukerdialog:kontrakt} mangler både en {@code OppgaveType}-verdi for bistand og en
 * {@code OppgavetypeDataDto}-subtype for bekreftelse av bistandsavklaring, og kan ikke endres herfra.
 * All utledning og validering nedenfor speiler {@code BostedOppgaveOppretter} og kjøres som normalt, slik at
 * feil i saksbehandlers avklaring fanges opp allerede nå. Selve REST-kallet er erstattet med en advarsel i
 * loggen. Det kastes bevisst ikke exception, slik at behandlingsflyten ikke stopper.
 * Se {@code dokumentasjon/felles-vilkaarsavklaring-fase-1-plassholdere.md}.
 */
@Dependent
public class BistandOppgaveOppretter {

    private static final Logger logger = LoggerFactory.getLogger(BistandOppgaveOppretter.class);

    private final VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    @Inject
    public BistandOppgaveOppretter(VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        for (Etterlysning etterlysning : etterlysninger) {
            var periodeAvklaring = finnAvklaring(behandling, etterlysning);
            var ikkeOppfyltÅrsak = BistandsvilkårIkkeOppfyltÅrsak.fraKode(periodeAvklaring.getIkkeOppfyltÅrsakKode());
            validerÅrsak(ikkeOppfyltÅrsak, periodeAvklaring);

            sendOppgaveTilBruker(behandling, etterlysning, aktørId, ikkeOppfyltÅrsak);
        }
    }

    private VilkårPeriodeAvklaring finnAvklaring(Behandling behandling, Etterlysning etterlysning) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR)
            .stream()
            .flatMap(g -> g.getForeslåtteAvklaringer().stream())
            .filter(avklaring -> avklaring.getReferanse().equals(etterlysning.getGrunnlagsreferanse()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke periodeAvklaring for referanse: " + etterlysning.getGrunnlagsreferanse()));
    }

    private static void validerÅrsak(BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, VilkårPeriodeAvklaring periodeAvklaring) {
        if (ikkeOppfyltÅrsak == BistandsvilkårIkkeOppfyltÅrsak.AVKORTET) {
            throw new IllegalStateException("Det er ikke forventet at AVKORTET skal brukes på periode det skal varsles om. Det er antagelig feil i løsningen som gjør at saksbehandler kan sette denne årsaken her.");
        }
        if (ikkeOppfyltÅrsak == BistandsvilkårIkkeOppfyltÅrsak.UDEFINERT) {
            Objects.requireNonNull(periodeAvklaring.getFritekstTilVarsel(), "FritekstTilVarsel må være satt når årsak er " + ikkeOppfyltÅrsak);
        }
    }

    // TODO(fase 1 plassholder): Erstatt med reelt kall til UngBrukerdialogOppgaveKlient.opprettOppgave(...) når
    // ung-brukerdialog-kontrakten har OppgaveType og OppgavetypeDataDto-subtype for bistandsavklaring.
    private static void sendOppgaveTilBruker(Behandling behandling, Etterlysning etterlysning, AktørId aktørId, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        logger.warn("IKKE IMPLEMENTERT: oppgave til deltaker for bistandsavklaring ble ikke sendt til ung-brukerdialog-api."
                + " Mangler kontrakt (OppgaveType/OppgavetypeDataDto for bistand). behandlingId={}, etterlysningReferanse={}, periode={}, årsak={}, aktørIdSatt={}",
            behandling.getId(), etterlysning.getEksternReferanse(), etterlysning.getPeriode(), ikkeOppfyltÅrsak, aktørId != null);
    }
}
