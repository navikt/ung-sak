package no.nav.k9.sak.behandlingslager.notat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

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
    void skalOppretteNotatPåFagsak() {
        var fagsak = lagFagsak();
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        String tekst = "en tekst med litt notater";

        NotatEntitet notat = NotatBuilder.of(fagsak, false)
            .notatTekst(tekst)
            .skjult(false)
            .build();

        var notatId = notatRepository.lagre(notat);
        List<NotatEntitet> notatEntitets = notatRepository.hentForSakOgAktør(fagsak);
        assertThat(notatEntitets).hasSize(1);

        NotatSakEntitet lagretNotat = (NotatSakEntitet) notatEntitets.get(0);
        assertThat(lagretNotat.getUuid()).isEqualTo(notatId);
        assertThat(lagretNotat.getNotatTekst()).isEqualTo(tekst);
        assertThat(lagretNotat.getFagsakId()).isEqualTo(fagsakId);
        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("VL");
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getVersjon()).isEqualTo(0);

    }


    @Test
    void skalOppretteNotatPåAktør() {
        var fagsak = lagFagsakMedPleietrengende();
        String tekst = "en tekst med litt notater på aktør";

        NotatEntitet notat = NotatBuilder.of(fagsak, true)
            .notatTekst(tekst)
            .skjult(false)
            .build();

        var notatId = notatRepository.lagre(notat);
        List<NotatEntitet> notatEntitets = notatRepository.hentForSakOgAktør(fagsak);
        assertThat(notatEntitets).hasSize(1);

        NotatAktørEntitet lagretNotat = (NotatAktørEntitet) notatEntitets.get(0);
        assertThat(lagretNotat.getUuid()).isEqualTo(notatId);
        assertThat(lagretNotat.getNotatTekst()).isEqualTo(tekst);
        assertThat(lagretNotat.getAktørId()).isEqualTo(fagsak.getPleietrengendeAktørId());
        assertThat(lagretNotat.getYtelseType()).isEqualTo(fagsak.getYtelseType());
        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("VL");
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getVersjon()).isEqualTo(0);

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

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER, personinfo.getAktørId(), new Saksnummer("A123"), LocalDate.now(), LocalDate.now());
        return fagsak;

    }

    private Fagsak lagFagsakMedPleietrengende() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();

        final Personinfo pleietrengende = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, personinfo.getAktørId(), pleietrengende.getAktørId(), null, new Saksnummer("A123"), LocalDate.now(), LocalDate.now());
        return fagsak;

    }
}
