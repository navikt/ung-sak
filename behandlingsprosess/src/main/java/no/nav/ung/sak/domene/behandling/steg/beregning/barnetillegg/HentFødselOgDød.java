package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.sak.typer.AktørId;

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

        var personFraPdl = pdlKlient.hentPerson(query, projection, List.of(Behandlingsnummer.UNGDOMSYTELSEN));

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

}
