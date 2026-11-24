package no.nav.ung.sak.mottak.dokumentmottak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.behandling.revurdering.KontrollbehandlingRuting;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallellBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse.OppgaveBekreftelseParser;

/**
 * Fordeler mottatte dokumenter mellom ordinær behandling og kontrollbehandling.
 * <ul>
 *     <li>Inntektsrapportering følger kontrollen av inntekt, se {@link KontrollbehandlingRuting#skalInntektsrapporteringTilKontrollbehandling}.</li>
 *     <li>Svar på etterlysning går til kontrollbehandlingen bare når den eier etterlysningen og ikke er avsluttet. Uttalelsen
 *     lagres uansett på behandlingen som eier etterlysningen. Et sent svar til en avsluttet kontrollbehandling går til
 *     ordinær behandling, som sene svar gjør i dag.</li>
 *     <li>Søknader og andre dokumenter går alltid til ordinær behandling.</li>
 * </ul>
 * Kallet må skje under fagsaklåsen, slik at svaret ikke endres før dokumentene er knyttet til behandling.
 */
@Dependent
public class MottattDokumentRuting {

    public enum MottakendeBehandling {
        ORDINÆR,
        KONTROLL
    }

    private KontrollbehandlingRuting kontrollbehandlingRuting;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private OppgaveBekreftelseParser oppgaveBekreftelseParser;
    private EtterlysningRepository etterlysningRepository;

    MottattDokumentRuting() {
        // for CDI proxy
    }

    @Inject
    public MottattDokumentRuting(KontrollbehandlingRuting kontrollbehandlingRuting,
                                 ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                 OppgaveBekreftelseParser oppgaveBekreftelseParser,
                                 EtterlysningRepository etterlysningRepository) {
        this.kontrollbehandlingRuting = kontrollbehandlingRuting;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.oppgaveBekreftelseParser = oppgaveBekreftelseParser;
        this.etterlysningRepository = etterlysningRepository;
    }

    /**
     * @return dokumentene gruppert på mottakende behandling, med ordinær behandling først. Grupper uten dokumenter er ikke med.
     */
    public Map<MottakendeBehandling, List<MottattDokument>> fordel(Fagsak fagsak, Collection<MottattDokument> dokumenter) {
        var harRapportering = dokumenter.stream().anyMatch(MottattDokumentRuting::erInntektsrapportering);
        var harSvar = dokumenter.stream().anyMatch(MottattDokumentRuting::erSvarPåEtterlysning);
        var rapporteringTilKontroll = harRapportering && kontrollbehandlingRuting.skalInntektsrapporteringTilKontrollbehandling(fagsak);
        Optional<Long> åpenKontrollbehandlingId = harSvar
            ? parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(fagsak.getId()).map(ParallellBehandling::behandlingId)
            : Optional.empty();

        var fordelt = new EnumMap<MottakendeBehandling, List<MottattDokument>>(MottakendeBehandling.class);
        for (var dokument : dokumenter) {
            var mottaker = finnMottaker(dokument, rapporteringTilKontroll, åpenKontrollbehandlingId);
            fordelt.computeIfAbsent(mottaker, m -> new ArrayList<>()).add(dokument);
        }
        return fordelt;
    }

    private MottakendeBehandling finnMottaker(MottattDokument dokument, boolean rapporteringTilKontroll, Optional<Long> åpenKontrollbehandlingId) {
        if (erInntektsrapportering(dokument)) {
            return rapporteringTilKontroll ? MottakendeBehandling.KONTROLL : MottakendeBehandling.ORDINÆR;
        }
        if (erSvarPåEtterlysning(dokument) && åpenKontrollbehandlingId.isPresent()) {
            var eier = finnBehandlingSomEierEtterlysningen(dokument);
            return åpenKontrollbehandlingId.get().equals(eier) ? MottakendeBehandling.KONTROLL : MottakendeBehandling.ORDINÆR;
        }
        return MottakendeBehandling.ORDINÆR;
    }

    private Long finnBehandlingSomEierEtterlysningen(MottattDokument dokument) {
        var oppgaveReferanse = oppgaveBekreftelseParser.parseOppgaveBekreftelse(dokument).getBekreftelse().getOppgaveReferanse();
        return etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveReferanse).getBehandlingId();
    }

    private static boolean erInntektsrapportering(MottattDokument dokument) {
        return Brevkode.INNTEKTSRAPPORTERING_TYPER.contains(dokument.getType());
    }

    private static boolean erSvarPåEtterlysning(MottattDokument dokument) {
        return Brevkode.VARSELUTTALELSE_TYPER.contains(dokument.getType());
    }
}
