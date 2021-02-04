package no.nav.k9.sak.domene.person.pdl;

import static java.util.function.Predicate.not;
import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE6;
import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE7;
import static no.nav.pdl.AdressebeskyttelseGradering.UGRADERT;

import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.typer.AktørId;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.GeografiskTilknytningResponseProjection;
import no.nav.pdl.GtType;
import no.nav.pdl.HentGeografiskTilknytningQueryRequest;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;

@ApplicationScoped
public class TilknytningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilknytningTjeneste.class);

    private PdlKlient pdlKlient;

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

    public void hentGeografiskTilknytning(AktørId aktørId, GeografiskTilknytning geografiskTilknytningFraTps) {
        try {
            var queryGT = new HentGeografiskTilknytningQueryRequest();
            queryGT.setIdent(aktørId.getId());
            var projectionGT = new GeografiskTilknytningResponseProjection()
                .gtType().gtBydel().gtKommune().gtLand();

            var geografiskTilknytning = pdlKlient.hentGT(queryGT, projectionGT);

            var diskresjonskodeFraPdl = hentDiskresjonskode(aktørId);
            var tilknytningFraPdl = getTilknytning(geografiskTilknytning);

            if (Objects.equals(geografiskTilknytningFraTps.getDiskresjonskode(), diskresjonskodeFraPdl)) {
                LOG.info("K9SAK PDL diskresjonskode: like svar");
            } else {
                LOG.info("K9SAK PDL diskresjonskode: ulike svar TPS->PDL {} {}", geografiskTilknytningFraTps.getDiskresjonskode(), diskresjonskodeFraPdl);
            }
            if (Objects.equals(geografiskTilknytningFraTps.getTilknytning(), tilknytningFraPdl)) {
                LOG.info("K9SAK PDL tilknytning: like svar");
            } else {
                LOG.info("K9SAK PDL tilknytning: ulike svar TPS->PDL {} {}", geografiskTilknytningFraTps.getTilknytning(), tilknytningFraPdl);
            }
        } catch (Exception e) {
            LOG.info("K9SAK PDL geografiskTilknytning: error", e);
        }
    }

    private Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection);

        return diskresjonskodeFor(person.getAdressebeskyttelse().stream());
    }

    private String getTilknytning(no.nav.pdl.GeografiskTilknytning gt) {
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

}
