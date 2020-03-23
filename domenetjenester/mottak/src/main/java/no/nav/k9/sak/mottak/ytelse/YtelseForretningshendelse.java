package no.nav.k9.sak.mottak.ytelse;

import java.time.LocalDate;

import no.nav.k9.sak.behandlingslager.hendelser.Forretningshendelse;
import no.nav.k9.sak.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.k9.sak.typer.AktørId;

public class YtelseForretningshendelse extends Forretningshendelse {
    private AktørId aktørId;
    private LocalDate fom;

    public YtelseForretningshendelse(ForretningshendelseType forretningshendelseType) {
        super(forretningshendelseType);
    }

    public YtelseForretningshendelse(ForretningshendelseType forretningshendelseType, String aktørId, LocalDate fom) {
        super(forretningshendelseType);
        this.aktørId = new AktørId(aktørId);
        this.fom = fom;

    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getFom() {
        return fom;
    }
}
