package no.nav.ung.sak.domene.person.pdl;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.typer.AktørId;

import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static no.nav.k9.felles.integrasjon.pdl.AdressebeskyttelseGradering.UGRADERT;
import static no.nav.ung.kodeverk.person.Diskresjonskode.KODE6;
import static no.nav.ung.kodeverk.person.Diskresjonskode.KODE7;

@ApplicationScoped
public class TilknytningTjeneste {
    private PdlKlient pdlKlient;

    @SuppressWarnings("unused")
    TilknytningTjeneste() {
        // CDI
    }

    @Inject
    public TilknytningTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    static Diskresjonskode diskresjonskodeFor(Stream<Adressebeskyttelse> adressebeskyttelse) {
        return adressebeskyttelse
            .map(Adressebeskyttelse::getGradering)
            .filter(not(UGRADERT::equals))
            .findFirst()
            .map(TilknytningTjeneste::tilDiskresjonskode)
            .orElse(null);
    }

    private static Diskresjonskode tilDiskresjonskode(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG -> KODE6;
            case FORTROLIG -> KODE7;
            case UGRADERT -> null;
        };
    }

    private String getTilknytning(no.nav.k9.felles.integrasjon.pdl.GeografiskTilknytning gt) {
        if (gt == null || gt.getGtType() == null)
            return null;
        var gtType = gt.getGtType();
        if (GtType.BYDEL.equals(gtType))
            return gt.getGtBydel();
        if (GtType.KOMMUNE.equals(gtType))
            return gt.getGtKommune();
        if (GtType.UTLAND.equals(gtType))
            return gt.getGtLand();
        return null;
    }

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId, FagsakYtelseType ytelseType) {
        var queryGT = new HentGeografiskTilknytningQueryRequest();
        queryGT.setIdent(aktørId.getId());
        var projectionGT = new GeografiskTilknytningResponseProjection()
            .gtType().gtBydel().gtKommune().gtLand();

        var diskresjon = hentDiskresjonskode(aktørId, ytelseType);
        var tilknytning = getTilknytning(pdlKlient.hentGeografiskTilknytning(queryGT, projectionGT, BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(ytelseType)));
        return new GeografiskTilknytning(tilknytning, diskresjon);
    }

    private Diskresjonskode hentDiskresjonskode(AktørId aktørId, FagsakYtelseType ytelseType) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = pdlKlient.hentPerson(query, projection, BehandlingsnummerMapper.ytelsestypeTilBehandlingsnummer(ytelseType));

        return diskresjonskodeFor(person.getAdressebeskyttelse().stream());
    }
}
