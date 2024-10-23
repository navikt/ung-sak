package no.nav.k9.sak.ytelse.ung.beregning.barnetillegg;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Doedsfall;
import no.nav.k9.felles.integrasjon.pdl.DoedsfallResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.FoedselsdatoResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class HentFødselOgDød {

    private final PdlKlient pdlKlient;

    @Inject
    public HentFødselOgDød(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public FødselOgDødInfo hentFødselOgDødInfo(AktørId aktørId) {

        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getAktørId());
        var projection = new PersonResponseProjection()
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato());

        var personFraPdl = pdlKlient.hentPerson(query, projection);

        var fødselsdato = personFraPdl.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        return new FødselOgDødInfo(aktørId, fødselsdato, dødssdato);
    }

    public record FødselOgDødInfo(
        AktørId aktørId,
        LocalDate fødselsdato,
        LocalDate dødsdato
    ) {
    }

}
