package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.DatoEndring;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretFomDatoBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.periodeendring.EndretTomDatoBekreftelse;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringType;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_FOM_DATO)
@OppgaveTypeRef(Bekreftelse.Type.UNG_ENDRET_TOM_DATO)
public class DatoEndringBekreftelseHåndterer implements BekreftelseHåndterer {


    private final UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;

    @Inject
    public DatoEndringBekreftelseHåndterer(
        UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold oppgaveBekreftelse) {
        DatoEndring bekreftelse = oppgaveBekreftelse.oppgaveBekreftelse().getBekreftelse();

        final var bekreftetPeriodeEndring = new UngdomsprogramBekreftetPeriodeEndring(
            bekreftelse.getNyDato(),
            oppgaveBekreftelse.journalpostId(),
            finnBekreftetPeriodeEndring(bekreftelse));

        var behandling = oppgaveBekreftelse.behandling();
        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), bekreftetPeriodeEndring);
    }

    private static UngdomsprogramPeriodeEndringType finnBekreftetPeriodeEndring(DatoEndring bekreftelse) {
        if (bekreftelse instanceof EndretTomDatoBekreftelse) {
            return UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO;
        } else if (bekreftelse instanceof EndretFomDatoBekreftelse) {
            return UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO;
        }
        throw new IllegalArgumentException("Kunne ikke håndtere bekreftelse av type " + bekreftelse.getType());
    }

}
