package no.nav.ung.sak.web.app.tjenester.fordeling;

import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.Kildesystem;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Journalpost;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.k9.søknad.ytelse.ung.v1.UngSøknadstype;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

record Papirsøknadopplysninger(
    NorskIdentitetsnummer norskIdentitetsnummer,
    LocalDate startdato,
    UUID deltakelseId,
    JournalpostId journalpostId
) {}

public class PapirsøknadtilK9FormatSøknadMapper {

    public static Søknad mapTilSøknad(Papirsøknadopplysninger papirsøknadopplysninger) {
        return new Søknad()
            .medVersjon(Versjon.of("1.0.0"))
            .medMottattDato(ZonedDateTime.now())
            .medSpråk(Språk.NORSK_BOKMÅL)
            .medKildesystem(Kildesystem.PUNSJ)
            .medSøknadId(SøknadId.of(UUID.randomUUID().toString()))
            .medSøker(new Søker(papirsøknadopplysninger.norskIdentitetsnummer()))
            .medJournalpost(new Journalpost()
                .medJournalpostId(papirsøknadopplysninger.journalpostId().getVerdi())
                .medInneholderMedisinskeOpplysninger(false)
                .medInformasjonSomIkkeKanPunsjes(false)
            )
            .medYtelse(new Ungdomsytelse()
                .medStartdato(papirsøknadopplysninger.startdato())
                .medSøknadType(UngSøknadstype.DELTAKELSE_SØKNAD)
                .medDeltakelseId(papirsøknadopplysninger.deltakelseId())
            );
    }
}
