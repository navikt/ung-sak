package no.nav.k9.sak.domene.person.pdl;

import java.util.Objects;

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
import no.nav.pdl.Person;
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
                LOG.info("K9SAK PDL diskresjonskode: ulike svar");
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

    public Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(query, projection);

        return getDiskresjonskode(person);
    }

    private Diskresjonskode getDiskresjonskode(Person person) {
        var kode = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
            .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6;
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7 : Diskresjonskode.UDEFINERT;
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
