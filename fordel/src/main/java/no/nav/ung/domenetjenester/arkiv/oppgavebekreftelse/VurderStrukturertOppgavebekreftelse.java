package no.nav.ung.domenetjenester.arkiv.oppgavebekreftelse;


import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.DatoEndring;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;
import no.nav.ung.domenetjenester.sak.FinnEllerOpprettUngSakTask;
import no.nav.ung.fordel.handler.MottattMelding;

public class VurderStrukturertOppgavebekreftelse {

    public MottattMelding håndtertOppgavebekreftelse(MottattMelding dataWrapper, String payload) {
        return håndterStrukturertJsonDokument(dataWrapper, payload);
    }

    private MottattMelding håndterStrukturertJsonDokument(MottattMelding dataWrapper, String payload) {
        // kun ny søknadsformat per nå
        var oppgaveBekreftelse = JsonUtils.fromString(payload, OppgaveBekreftelse.class);
        var bekreftelse = oppgaveBekreftelse.getBekreftelse();
        // TODO: Gjer validering her

        var endretDato = bekreftelse instanceof DatoEndring endring ? endring.getNyDato() : null;
        if (endretDato != null) {
            dataWrapper.setFørsteUttaksdag(endretDato);
        } else {
            dataWrapper.setFørsteUttaksdag(oppgaveBekreftelse.getMottattDato().toLocalDate());
        }
        return dataWrapper.nesteSteg(FinnEllerOpprettUngSakTask.TASKTYPE);
    }




}
