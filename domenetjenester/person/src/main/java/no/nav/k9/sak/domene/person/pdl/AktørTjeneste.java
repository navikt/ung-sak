package no.nav.k9.sak.domene.person.pdl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.jboss.weld.util.collections.ImmutableList.of;

import java.util.List;
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
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class AktørTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AktørTjeneste.class);
    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(8, TimeUnit.HOURS);

    private LRUCache<AktørId, PersonIdent> cacheAktørIdTilIdent;
    private LRUCache<PersonIdent, AktørId> cacheIdentTilAktørId;

    private PdlKlient pdlKlient;

    @SuppressWarnings("unused")
    AktørTjeneste() {
        // CDI
    }

    @Inject
    public AktørTjeneste(PdlKlient pdlKlient) {
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

        Optional<AktørId> aktørId = hentAktørIdFraPDL(personIdent.getIdent());
            aktørId.ifPresent(aid -> {
                    // Kan ikke legge til i cache aktørId -> ident ettersom ident kan være ikke-current
                    cacheIdentTilAktørId.put(personIdent, aid);
                }
            );
            return aktørId;
    }

    public Optional<PersonIdent> hentPersonIdentForAktørId(AktørId aktørId) {
        var personIdentFraCache = ofNullable(cacheAktørIdTilIdent.get(aktørId));
        if (personIdentFraCache.isPresent()) {
            return personIdentFraCache;
        }

        Optional<PersonIdent> personIdent = hentPersonIdentFraPDL(aktørId.getId());
        personIdent.ifPresent(i -> {
                cacheAktørIdTilIdent.put(aktørId, i);
                cacheIdentTilAktørId.put(i, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
            }
        );
        return personIdent;
    }

    private Optional<AktørId> hentAktørIdFraPDL(String personIdent) {
        return identerFor(personIdent, IdentGruppe.AKTORID).stream().findFirst().map(IdentInformasjon::getIdent).map(AktørId::new);
    }

    private Optional<PersonIdent> hentPersonIdentFraPDL(String aktørId) {
        var personIdenter = identerFor(aktørId, IdentGruppe.FOLKEREGISTERIDENT);
        return personIdenter.stream().findFirst().map(IdentInformasjon::getIdent).map(PersonIdent::new);

    }

    private List<IdentInformasjon> identerFor(String ident, IdentGruppe identGruppe) {
        HentIdenterQueryRequest request = new HentIdenterQueryRequest();
        request.setIdent(ident);
        request.setGrupper(of(identGruppe));
        request.setHistorikk(Boolean.FALSE);

        IdentlisteResponseProjection projeksjon = new IdentlisteResponseProjection()
            .identer(
                new IdentInformasjonResponseProjection()
                    .ident()
            );

        try {
            return pdlKlient.hentIdenter(request, projeksjon).getIdenter();
        } catch (VLException e) {
            if (PdlKlient.PDL_KLIENT_NOT_FOUND_KODE.equals(e.getKode())) {
                log.info("Ident av type {} ikke funnet for ident {}", identGruppe, ident);
                return emptyList();
            }
            throw e;
        }
    }
}
