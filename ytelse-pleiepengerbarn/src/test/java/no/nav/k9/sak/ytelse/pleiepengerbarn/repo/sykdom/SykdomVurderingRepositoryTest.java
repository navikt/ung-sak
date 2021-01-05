package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.Collections;

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
    SykdomVurderingRepository repo;



    @Test
    void lagrePerson() {
        SykdomPerson p = new SykdomPerson(
            new AktørId(123456789L),
            "11111111111"
        );
        repo.lagreEllerOppdater(p);
    }

    @Test
    void lagreVurdering() {
        SykdomPerson person = new SykdomPerson(
            new AktørId(123456789L),
            "11111111111"
        );
        person = repo.lagreEllerOppdater(person);

        SykdomVurderinger vurderinger = new SykdomVurderinger(
            person,
            "test",
            LocalDateTime.now()
        );

        SykdomVurdering vurdering = new SykdomVurdering(
            SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE,
            1L,
            vurderinger,
            Collections.emptyList(),
            1L,
            "test",
            LocalDateTime.now()
            );

        repo.lagre(vurderinger);
        repo.lagre(vurdering);
    }
}
