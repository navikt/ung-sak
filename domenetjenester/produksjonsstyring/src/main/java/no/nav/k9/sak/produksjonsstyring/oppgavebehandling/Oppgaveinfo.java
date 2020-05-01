package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import java.util.Objects;

import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;

public class Oppgaveinfo {

    public static final Oppgaveinfo VURDER_KONST_YTELSE = new Oppgaveinfo(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode(), null);
    public static final Oppgaveinfo VURDER_DOKUMENT = new Oppgaveinfo(OppgaveÅrsak.VURDER_DOKUMENT_VL.getKode(), null);

    private String oppgaveType;
    private String status;

    public Oppgaveinfo(String oppgaveType, String status) {
        this.oppgaveType = oppgaveType;
        this.status = status;
    }

    public String getOppgaveType() {
        return oppgaveType;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Oppgaveinfo) {
            return oppgaveType.equals(((Oppgaveinfo) obj).getOppgaveType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(oppgaveType);
    }
}
