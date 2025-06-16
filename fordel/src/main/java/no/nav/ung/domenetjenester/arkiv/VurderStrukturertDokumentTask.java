package no.nav.ung.domenetjenester.arkiv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.domenetjenester.arkiv.oppgavebekreftelse.VurderStrukturertOppgavebekreftelse;
import no.nav.ung.domenetjenester.arkiv.soknad.VurderStrukturertSøknad;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.kodeverk.dokument.Brevkode;

@ProsessTask(VurderStrukturertDokumentTask.TASKTYPE)
@ApplicationScoped
public class VurderStrukturertDokumentTask extends WrappedProsessTaskHandler {
    public static final String TASKTYPE = "arkiv.vurderStrukturertDokument";


    @Inject
    public VurderStrukturertDokumentTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste) {
        super(fordelProsessTaskTjeneste);
    }

    @Override
    public void precondition(MottattMelding dataWrapper) {
        assertInneholderStrukturertDokument(dataWrapper);
    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        final String payload = dataWrapper.getPayloadAsString().orElseThrow(() -> new IllegalArgumentException("Må ha payload for å vurdere strukturert dokument"));
        if (erOppgaveBekreftelse(dataWrapper)) {
            return new VurderStrukturertOppgavebekreftelse().håndtertOppgavebekreftelse(dataWrapper);
        }
        return new VurderStrukturertSøknad().håndtertSøknad(dataWrapper, payload);

    }

    private static boolean erOppgaveBekreftelse(MottattMelding dataWrapper) {
        return dataWrapper.getBrevkode() != null && dataWrapper.getBrevkode().equals(Brevkode.UNGDOMSYTELSE_OPPGAVE_BEKREFTELSE.getOffisiellKode());
    }

    private void assertInneholderStrukturertDokument(MottattMelding dataWrapper) {
        var optPayload = dataWrapper.getPayloadAsString();
        if (optPayload.isEmpty()) {
            throw new IllegalStateException("Forventer strukturert dokument når denne tasken behandles. Har den blitt sendt til denne tasken ved en feil?");
        }
    }


    public static boolean erJson(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("{");
    }
}
