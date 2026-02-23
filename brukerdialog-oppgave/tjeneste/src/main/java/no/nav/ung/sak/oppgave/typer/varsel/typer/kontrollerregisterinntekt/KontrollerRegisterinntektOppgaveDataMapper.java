package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_AVVIK_REGISTERINNTEKT)
public class KontrollerRegisterinntektOppgaveDataMapper implements OppgaveDataMapper {

    protected KontrollerRegisterinntektOppgaveDataMapper() {
        // CDI proxy
    }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
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
        registerinntekt.arbeidOgFrilansInntekter()
            .forEach(i -> entitet.leggTilArbeidOgFrilansInntekt(i.arbeidsgiver(), i.inntekt()));
        registerinntekt.ytelseInntekter()
            .forEach(i -> entitet.leggTilYtelseInntekt(i.ytelsetype(), i.inntekt()));
        return entitet;
    }
}
