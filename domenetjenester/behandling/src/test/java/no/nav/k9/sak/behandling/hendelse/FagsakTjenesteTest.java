package no.nav.k9.sak.behandling.hendelse;

import static java.time.Month.JANUARY;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FagsakTjenesteTest {

    private final AktørId forelderAktørId = AktørId.dummy();

    @Inject
    private EntityManager entityManager;
    private FagsakTjeneste tjeneste;
    @Mock
    private SøknadRepository søknadRepository;
    private Fagsak fagsak;
    private Personinfo personinfo;
    private LocalDate forelderFødselsdato = LocalDate.of(1990, JANUARY, 1);

    private final FagsakYtelseType ytelseType = FagsakYtelseType.FORELDREPENGER;

    @BeforeEach
    public void oppsett() {
        tjeneste = new FagsakTjeneste(new BehandlingRepositoryProvider(entityManager), null);

        personinfo = new Personinfo.Builder()
            .medAktørId(forelderAktørId)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medNavn("Kari Nordmann")
            .medFødselsdato(forelderFødselsdato)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medForetrukketSpråk(Språkkode.nb)
            .build();

        Fagsak fagsak = lagNyFagsak(personinfo);

        this.fagsak = fagsak;
    }

    private Fagsak lagNyFagsak(Personinfo personinfo) {
        Fagsak fagsak = Fagsak.opprettNy(ytelseType, personinfo.getAktørId());
        tjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

    @Test
    public void opprettFlereFagsakerSammeBrukerDuplikaterSkalFeile_1() throws Exception {
        Assertions.assertThrows(PersistenceException.class, () -> {
            // Opprett en fagsak i systemet
            Whitebox.setInternalState(fagsak, "fagsakStatus", FagsakStatus.LØPENDE); // dirty, men eksponerer ikke status nå

            Fagsak fagsakNy = Fagsak.opprettNy(ytelseType, personinfo.getAktørId());
            tjeneste.opprettFagsak(fagsakNy);
        });
    }
}
