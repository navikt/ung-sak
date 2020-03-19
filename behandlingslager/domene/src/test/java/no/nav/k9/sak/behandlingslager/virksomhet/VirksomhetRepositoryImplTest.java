package no.nav.k9.sak.behandlingslager.virksomhet;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;

public class VirksomhetRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private VirksomhetRepository repository = new VirksomhetRepository();

    @Test
    public void skal_lagre_virksomheten() {
        VirksomhetEntitet.Builder builder = new VirksomhetEntitet.Builder();
        final String orgnr = "123123123";
        final Virksomhet virksomheten = builder.medOrgnr(orgnr).medNavn("Virksomheten").medOppstart(LocalDate.now()).oppdatertOpplysningerNå().build();
        repository.lagre(virksomheten);

        Optional<Virksomhet> hent = repository.hent(orgnr);
        assertThat(hent).isPresent();

        Virksomhet virksomhet = hent.get();

        builder = new VirksomhetEntitet.Builder(virksomhet);
        builder.oppdatertOpplysningerNå();

        repository.lagre(builder.build());

        hent = repository.hent(orgnr);
        assertThat(hent).isPresent();
    }
}
