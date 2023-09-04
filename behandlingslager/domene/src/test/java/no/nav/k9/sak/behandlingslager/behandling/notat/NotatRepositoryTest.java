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
import no.nav.k9.sak.behandlingslager.notat.Notat;
import no.nav.k9.sak.behandlingslager.notat.NotatBuilder;
import no.nav.k9.sak.behandlingslager.notat.NotatRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatSakEntitet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class NotatRepositoryTest {

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

        Notat notat = NotatBuilder.of(fagsak, false)
            .notatTekst(tekst)
            .skjult(false)
            .build();

        var notatId = notatRepository.opprett(notat);
        NotatSakEntitet lagretNotat = (NotatSakEntitet) notatRepository.hent(notatId);
        assertThat(lagretNotat.getNotatTekst()).isEqualTo(tekst);
        assertThat(lagretNotat.getFagsakId()).isEqualTo(fagsakId);
        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("VL");
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getVersjon()).isEqualTo(0);

        //TODO sjekk at fagsak notat har verdi og aktør notat mangler verdi

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
