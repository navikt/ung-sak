package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PleietrengendeSykdomVurderingRepositoryTest {
    @Inject
    private SykdomVurderingRepository repo;

    @Test
    void lagrePerson() {
        Person p = new Person(
            new AktørId(123456789L),
            "11111111111"
        );
        repo.hentEllerLagre(p);
    }

    @Test
    void lagreVurderingGårBra() {
        lagreVurdering(new AktørId("023456789"));
    }

    private void lagreVurdering(AktørId barnAktørId) {
        final Person barn = repo.hentEllerLagre(new Person(
            barnAktørId,
            barnAktørId.getId()
        ));

        assertThat(barn).isNotNull();
        assertThat(barn.getId()).isNotNull();

        final PleietrengendeSykdom vurderinger = repo.hentEllerLagre(new PleietrengendeSykdom(
            barn,
            "test",
            LocalDateTime.now()
        ));

        final PleietrengendeSykdomVurdering vurdering = new PleietrengendeSykdomVurdering(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Collections.emptyList(),
            "test",
            LocalDateTime.now()
            );
        vurdering.setRangering(1L);

        final Person mor = repo.hentEllerLagre(new Person(
            new AktørId("222456789"),
            "22211111111"
        ));

        final PleietrengendeSykdomVurderingVersjon versjon1 = new PleietrengendeSykdomVurderingVersjon(
            vurdering,
            "Lorem Ipsum",
            Resultat.IKKE_OPPFYLT,
            Long.valueOf(1L),
            "",
            LocalDateTime.now(),
            UUID.randomUUID(),
            "abc",
            mor,
            null,
            Collections.emptyList(),
            Arrays.asList()
        );

        final PleietrengendeSykdomVurderingVersjon versjon2 = new PleietrengendeSykdomVurderingVersjon(
                vurdering,
                "Lorem Ipsum",
                Resultat.OPPFYLT,
                Long.valueOf(2L),
                "",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "abc",
                mor,
                null,
                Collections.emptyList(),
                Arrays.asList()
            );

        repo.lagre(vurdering, vurderinger);
        repo.lagre(versjon1);
        repo.lagre(versjon2);
    }

    @Test
    void lagreVurderingVersjonBesluttet() throws Exception {
        AktørId barnAktørId = new AktørId("023456789");
        final Person barn = repo.hentEllerLagre(new Person(
            barnAktørId,
            barnAktørId.getId()
        ));
        final PleietrengendeSykdom vurderinger = repo.hentEllerLagre(new PleietrengendeSykdom(
            barn,
            "test",
            LocalDateTime.now()
        ));

        final PleietrengendeSykdomVurdering vurdering = new PleietrengendeSykdomVurdering(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Collections.emptyList(),
            "test",
            LocalDateTime.now()
        );
        vurdering.setRangering(1L);

        final Person mor = repo.hentEllerLagre(new Person(
            new AktørId("222456789"),
            "22211111111"
        ));

        final PleietrengendeSykdomVurderingVersjon vurderingVersjonUtenBesluttet = new PleietrengendeSykdomVurderingVersjon(
            vurdering,
            "Lorem Ipsum",
            Resultat.IKKE_OPPFYLT,
            Long.valueOf(1L),
            "",
            LocalDateTime.now(),
            UUID.randomUUID(),
            "abc",
            mor,
            null,
            Collections.emptyList(),
            Arrays.asList()
        );

        repo.lagre(vurdering, vurderinger);
        repo.lagre(vurderingVersjonUtenBesluttet);
        PleietrengendeSykdomVurderingVersjonBesluttet besluttet = new PleietrengendeSykdomVurderingVersjonBesluttet("test", LocalDateTime.now(), vurderingVersjonUtenBesluttet);
        repo.lagre(besluttet);

        boolean erBesluttet = repo.hentErBesluttet(vurderingVersjonUtenBesluttet);

        assertThat(erBesluttet);
    }

    @Test
    void hentVurderinger() {
        final AktørId barn1 = new AktørId("123456787");
        final AktørId barn2 = new AktørId("123456788");

        verifyAntallVurderingerPåBarn(barn1, 0);
        verifyAntallVurderingerPåBarn(barn2, 0);
        lagreVurdering(barn2);
        verifyAntallVurderingerPåBarn(barn1, 0);
        verifyAntallVurderingerPåBarn(barn2, 1);
        lagreVurdering(barn1);
        verifyAntallVurderingerPåBarn(barn1, 1);

        final Collection<PleietrengendeSykdomVurderingVersjon> vurderinger = repo.hentSisteVurderingerFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, barn1);
        assertThat(vurderinger.iterator().next().getResultat()).isEqualTo(Resultat.OPPFYLT);
    }

    private void verifyAntallVurderingerPåBarn(final AktørId barn, final int antall) {
        final Collection<PleietrengendeSykdomVurderingVersjon> vurderinger = repo.hentSisteVurderingerFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, barn);
        assertThat(vurderinger.size()).isEqualTo(antall);
    }
}
