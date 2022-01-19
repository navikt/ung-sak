package no.nav.k9.sak.domene.person.pdl;


import static java.util.function.Predicate.not;
import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE6;
import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE7;
import static no.nav.k9.felles.integrasjon.pdl.AdressebeskyttelseGradering.UGRADERT;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.felles.integrasjon.pdl.Adressebeskyttelse;
import no.nav.k9.felles.integrasjon.pdl.AdressebeskyttelseGradering;
import no.nav.k9.felles.integrasjon.pdl.AdressebeskyttelseResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.GeografiskTilknytningResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.GtType;
import no.nav.k9.felles.integrasjon.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentPersonQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.PersonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;

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
        switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND:
            case STRENGT_FORTROLIG:
                return KODE6;
            case FORTROLIG:
                return KODE7;
            default:
                return null;
        }
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

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        var queryGT = new HentGeografiskTilknytningQueryRequest();
        queryGT.setIdent(aktørId.getId());
        var projectionGT = new GeografiskTilknytningResponseProjection()
            .gtType().gtBydel().gtKommune().gtLand();

        var diskresjon = hentDiskresjonskode(aktørId);
        var tilknytning = getTilknytning(pdlKlient.hentGT(queryGT, projectionGT));
        return new GeografiskTilknytning(tilknytning, diskresjon);
    }

    private Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = pdlKlient.hentPerson(query, projection);

        return diskresjonskodeFor(person.getAdressebeskyttelse().stream());
    }
}
