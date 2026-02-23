package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_FJERNET_PERIODE)
public class FjernetPeriodeOppgaveDataMapper implements OppgaveDataMapper {

    private EntityManager entityManager;

    protected FjernetPeriodeOppgaveDataMapper() {
        // CDI proxy
    }

    @Inject
    public FjernetPeriodeOppgaveDataMapper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
        var dto = (FjernetPeriodeDataDto) data;
        var entitet = new FjernetPeriodeOppgaveDataEntitet(dto.forrigeStartdato(), dto.forrigeSluttdato());
        entityManager.persist(entitet);
        return entitet;
    }
}
