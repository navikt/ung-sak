package no.nav.k9.sak.behandlingslager.notat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
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
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("enSaksbehandler");

    }

    @AfterEach
    void tearDown() {
        SubjectHandlerUtils.reset();
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

        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("enSaksbehandler");
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getVersjon()).isEqualTo(0);

    }


    @Test
    void skalOppretteNotatPåAktør() {
        var fagsak = lagFagsakMedPleietrengende();
        fagsakRepository.opprettNy(fagsak);
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
        assertThat(lagretNotat.getOpprettetAv()).isEqualTo("enSaksbehandler");
        assertThat(lagretNotat.isAktiv()).isTrue();
        assertThat(lagretNotat.getVersjon()).isEqualTo(0);

    }


    @Test
    void skalSortereNotaterPåOpprettetTidspunkt() {
        var fagsak = lagFagsakMedPleietrengende();
        fagsakRepository.opprettNy(fagsak);

        var nå = LocalDateTime.now();
        NotatEntitet notat4 = NotatBuilder.of(fagsak, true).notatTekst("nyeste notat").build();
        notat4.overstyrOpprettetTidspunkt(nå);

        NotatEntitet notat3 = NotatBuilder.of(fagsak, false).notatTekst("nr 3").build();
        notat3.overstyrOpprettetTidspunkt(nå.minusSeconds(10));

        NotatEntitet notat2 = NotatBuilder.of(fagsak, true).notatTekst("nr 2").build();
        notat2.overstyrOpprettetTidspunkt(nå.minusMinutes(10));

        NotatEntitet notat1 = NotatBuilder.of(fagsak, false).notatTekst("eldste notat").build();
        notat1.overstyrOpprettetTidspunkt(nå.minusMonths(1));

        notatRepository.lagre(notat2);
        notatRepository.lagre(notat1);
        notatRepository.lagre(notat4);
        notatRepository.lagre(notat3);

        List<NotatEntitet> notatEntitets = notatRepository.hentForSakOgAktør(fagsak);

        assertThat(notatEntitets).extracting(NotatEntitet::getOpprettetTidspunkt)
            .containsExactlyElementsOf(
                List.of(
                    notat4.getOpprettetTidspunkt(),
                    notat3.getOpprettetTidspunkt(),
                    notat2.getOpprettetTidspunkt(),
                    notat1.getOpprettetTidspunkt()
                )
            );

    }

    @Test
    void skalFeileHvisEndrerPåUtdatertVersjon() {
        var fagsak = lagFagsakMedPleietrengende();
        fagsakRepository.opprettNy(fagsak);
        String tekst = "en tekst med litt notater på aktør";

        //Klient 1 lager notat
        NotatEntitet originalNotat = NotatBuilder.of(fagsak, true)
            .notatTekst(tekst)
            .skjult(false)
            .build();
        notatRepository.lagre(originalNotat);
        entityManager.detach(originalNotat);

        //Klient 1 endrer notat, men har ikke lagret
        var notatKopi1 = notatRepository.hentForSakOgAktør(fagsak).get(0);
        notatKopi1.nyTekst("endring som vil feile");
        entityManager.detach(notatKopi1);

        //Klient 2 endrer også samme notatet og rekker å lagre
        var notatKopi2 = notatRepository.hentForSakOgAktør(fagsak).get(0);
        notatKopi2.nyTekst("endring som går bra");
        notatRepository.lagre(notatKopi2); //persist + flush
        entityManager.detach(notatKopi2);

        //Klient 1 forsøker å lagre utdatert versjon
        assertThatThrownBy(() -> {
            entityManager.merge(notatKopi1);
            entityManager.flush();
        }).isInstanceOf(OptimisticLockException.class);


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
