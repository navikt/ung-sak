package no.nav.k9.sak.domene.person.pdl;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.aktør.klient.DetFinnesFlereAktørerMedSammePersonIdentException;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class AktørTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AktørTjeneste.class);

    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(8, TimeUnit.HOURS);

    private LRUCache<AktørId, PersonIdent> cacheAktørIdTilIdent;
    private LRUCache<PersonIdent, AktørId> cacheIdentTilAktørId;

    private AktørConsumer aktørConsumer;
    private PdlKlient pdlKlient;

    @SuppressWarnings("unused")
    AktørTjeneste() {
        // CDI
    }

    @Inject
    public AktørTjeneste(PdlKlient pdlKlient,
                         AktørConsumer aktørConsumer) {
        this.aktørConsumer = aktørConsumer;
        this.pdlKlient = pdlKlient;
        this.cacheAktørIdTilIdent = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);
        this.cacheIdentTilAktørId = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }

        var aktørIdFraCache = ofNullable(cacheIdentTilAktørId.get(personIdent));
        if (aktørIdFraCache.isPresent()) {
            return aktørIdFraCache;
        }
        try {
            Optional<AktørId> aktørIdFraTps = aktørConsumer.hentAktørIdForPersonIdent(personIdent.getIdent()).map(AktørId::new);
            aktørIdFraTps.ifPresent(aktørId -> {
                    hentAktørIdFraPDL(personIdent.getIdent(), aktørId.getId());
                    // Kan ikke legge til i cache aktørId -> ident ettersom ident kan være ikke-current
                    cacheIdentTilAktørId.put(personIdent, aktørId);
                }
            );
            return aktørIdFraTps;
        } catch (DetFinnesFlereAktørerMedSammePersonIdentException e) { // NOSONAR
            // Her sorterer vi ut dødfødte barn
            return Optional.empty();
        }
    }

    public Optional<PersonIdent> hentPersonIdentForAktørId(AktørId aktørId) {
        var personIdentFraCache = ofNullable(cacheAktørIdTilIdent.get(aktørId));
        if (personIdentFraCache.isPresent()) {
            return personIdentFraCache;
        }

        Optional<PersonIdent> personIdent = aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new);
        personIdent.ifPresent(i -> {
                hentPersonIdentFraPDL(aktørId.getId(), i.getIdent());
                cacheAktørIdTilIdent.put(aktørId, i);
                cacheIdentTilAktørId.put(i, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
            }
        );

        return personIdent;
    }

    private void hentAktørIdFraPDL(String fnr, String aktørFraConsumer) {
        try {
            var request = new HentIdenterQueryRequest();
            request.setIdent(fnr);
            request.setGrupper(List.of(IdentGruppe.AKTORID));
            request.setHistorikk(Boolean.FALSE);
            var projection = new IdentlisteResponseProjection()
                .identer(new IdentInformasjonResponseProjection().ident());
            //TODO Verifiser tema
            var identliste = pdlKlient.hentIdenter(request, projection, Tema.FOR);
            int antall = identliste.getIdenter().size();
            var aktørId = identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).orElse(null);

            if (antall == 1 && Objects.equals(aktørFraConsumer, aktørId)) {
                LOG.info("PDL fnr->aktørid: like aktørid");
            } else if (antall != 1 && Objects.equals(aktørFraConsumer, aktørId)) {
                LOG.info("PDL fnr->aktørid: ulikt antall aktørid {}", antall);
            } else {
                LOG.info("PDL fnr->aktørid: ulike aktørid TPS {} og PDL {} antall {}", aktørFraConsumer, aktørId, antall);
            }
        } catch (Exception e) {
            LOG.info("PDL fnr->aktørid error", e);
        }
    }

    private void hentPersonIdentFraPDL(String aktørId, String identFraConsumer) {
        try {
            var request = new HentIdenterQueryRequest();
            request.setIdent(aktørId);
            request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT));
            request.setHistorikk(Boolean.FALSE);
            var projection = new IdentlisteResponseProjection()
                .identer(new IdentInformasjonResponseProjection().ident());
            var identliste = pdlKlient.hentIdenter(request, projection, Tema.FOR);
            int antall = identliste.getIdenter().size();
            var fnr = identliste.getIdenter().stream().findFirst().map(IdentInformasjon::getIdent).orElse(null);
            if (antall == 1 && Objects.equals(identFraConsumer, fnr)) {
                LOG.info("PDL aktørid->fnr: like identer");
            } else if (antall != 1 && Objects.equals(identFraConsumer, fnr)) {
                LOG.info("PDL aktørid->fnr: ulikt antall identer {}", antall);
            } else {
                LOG.info("PDL aktørid->fnr: ulike identer TPS {} og PDL {} antall {}", identFraConsumer, fnr, antall);
            }
        } catch (Exception e) {
            LOG.info("PDL aktørid->fnr error", e);
        }
    }

}
