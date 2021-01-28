package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SykdomVurderingRepositoryTest {
    @Inject
    private SykdomVurderingRepository repo;

    @Test
    void lagrePerson() {
        SykdomPerson p = new SykdomPerson(
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
        final SykdomPerson barn = repo.hentEllerLagre(new SykdomPerson(
            barnAktørId,
            barnAktørId.getId()
        ));

        assertThat(barn).isNotNull();
        assertThat(barn.getId()).isNotNull();

        final SykdomVurderinger vurderinger = repo.hentEllerLagre(new SykdomVurderinger(
            barn,
            "test",
            LocalDateTime.now()
        ));

        final SykdomVurdering vurdering = new SykdomVurdering(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            Collections.emptyList(),
            "test",
            LocalDateTime.now()
            );
        vurdering.setRangering(1L);

        final SykdomPerson mor = repo.hentEllerLagre(new SykdomPerson(
            new AktørId("222456789"),
            "22211111111"
        ));

        final SykdomVurderingVersjon versjon1 = new SykdomVurderingVersjon(
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

        final SykdomVurderingVersjon versjon2 = new SykdomVurderingVersjon(
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

        final Collection<SykdomVurderingVersjon> vurderinger = repo.hentSisteVurderingerFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, barn1);
        assertThat(vurderinger.iterator().next().getResultat()).isEqualTo(Resultat.OPPFYLT);
    }

    private void verifyAntallVurderingerPåBarn(final AktørId barn, final int antall) {
        final Collection<SykdomVurderingVersjon> vurderinger = repo.hentSisteVurderingerFor(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, barn);
        assertThat(vurderinger.size()).isEqualTo(antall);
    }
}
