package no.nav.k9.sak.behandlingslager.behandling.notat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class NotatEntitetTest {

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private NotatRepository notatRepository;


    @BeforeEach
    void setup() {
        fagsakRepository = new FagsakRepository(entityManager);
        notatRepository = new NotatRepository(entityManager);
    }

    @Test
    void skalOppretteNotat() {
        var fagsak = lagFagsak();
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        String tekst = "en tekst med litt notater";

        NotatEntitet notat = new NotatBuilder()
            .notatTekst(tekst)
            .fagsakId(fagsakId)
            .gjelder(fagsak.getAktørId())
            .skjult(false)
            .build();

        Long notatId = notatRepository.opprett(notat);
        NotatEntitet lagretNotat = notatRepository.hent(notatId);
        assertThat(lagretNotat.getNotatTekst()).isEqualTo(tekst);
        assertThat(lagretNotat.getFagsakId()).isEqualTo(fagsakId);
        assertThat(lagretNotat.getGjelder()).isEqualTo(fagsak.getAktørId());
        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("VL");
        assertThat(lagretNotat.isSkjult()).isFalse();
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getErstattetAvNotatId()).isNull();

    }

    private Fagsak lagFagsak() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, personinfo.getAktørId(), new Saksnummer("A123"), LocalDate.now(), LocalDate.now());
        return fagsak;

    }
}
