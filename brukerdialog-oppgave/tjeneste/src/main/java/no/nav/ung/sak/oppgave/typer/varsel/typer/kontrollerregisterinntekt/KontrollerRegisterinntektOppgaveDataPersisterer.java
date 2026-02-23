package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveDataPersisterer;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_AVVIK_REGISTERINNTEKT)
public class KontrollerRegisterinntektOppgaveDataPersisterer implements OppgaveDataPersisterer {

    private EntityManager entityManager;

    protected KontrollerRegisterinntektOppgaveDataPersisterer() {
        // CDI proxy
    }

    @Inject
    public KontrollerRegisterinntektOppgaveDataPersisterer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDto data) {
        var dto = (KontrollerRegisterinntektOppgavetypeDataDto) data;
        var registerinntekt = dto.registerinntekt();
        var entitet = new KontrollerRegisterinntektOppgaveDataEntitet(
            dto.fraOgMed(),
            dto.tilOgMed(),
            dto.gjelderDelerAvMåned(),
            registerinntekt.totalInntektArbeidOgFrilans(),
            registerinntekt.totalInntektYtelse(),
            registerinntekt.totalInntekt()
        );
        oppgave.setOppgaveData(entitet);
        registerinntekt.arbeidOgFrilansInntekter()
            .forEach(i -> entitet.leggTilArbeidOgFrilansInntekt(i.arbeidsgiver(), i.inntekt()));
        registerinntekt.ytelseInntekter()
            .forEach(i -> entitet.leggTilYtelseInntekt(i.ytelsetype(), i.inntekt()));
        entityManager.persist(entitet);
        entityManager.flush();
    }
}
