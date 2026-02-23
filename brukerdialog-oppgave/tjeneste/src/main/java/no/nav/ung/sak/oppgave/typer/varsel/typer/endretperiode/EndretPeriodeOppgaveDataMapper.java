package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_PERIODE)
public class EndretPeriodeOppgaveDataMapper implements OppgaveDataMapper {


    protected EndretPeriodeOppgaveDataMapper() {
        // CDI proxy
    }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
        var dto = (EndretPeriodeDataDto) data;
        return new EndretPeriodeOppgaveDataEntitet(
            dto.nyPeriode() != null ? dto.nyPeriode().getFomDato() : null,
            dto.nyPeriode() != null ? dto.nyPeriode().getTomDato() : null,
            dto.forrigePeriode() != null ? dto.forrigePeriode().getFomDato() : null,
            dto.forrigePeriode() != null ? dto.forrigePeriode().getTomDato() : null,
            dto.endringer()
        );
    }
}
