package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import static no.nav.k9.kodeverk.vedtak.Vedtaksbrev.AUTOMATISK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VedtakVarselRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private Behandling behandling;
    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.OMSORGSPENGER);
    }

    private void lagre(VedtakVarsel v) {
        repository.lagre(v);
        repository.flushAndClear();
    }

    @Test
    public void skal_lagre_redusert_utbetaling_årsaker() {
        VedtakVarsel vedtakVarsel = lagVedtakVarsel();
        vedtakVarsel.setRedusertUtbetalingÅrsaker(Set.of("ÅRSAK_1", "ÅRSAK_2"));
        lagre(vedtakVarsel);

        Long id = vedtakVarsel.getId();
        assertThat(id).isNotNull();

        VedtakVarsel lagret = repository.hent(VedtakVarsel.class, id);
        assertThat(lagret.getRedusertUtbetalingÅrsaker()).isEqualTo(vedtakVarsel.getRedusertUtbetalingÅrsaker());
    }

    @Test
    public void skal_håndtere_ingen_redusert_utbetaling_årsaker() {
        VedtakVarsel vedtakVarsel = lagVedtakVarsel();
        lagre(vedtakVarsel);

        Long id = vedtakVarsel.getId();
        assertThat(id).isNotNull();

        VedtakVarsel lagret = repository.hent(VedtakVarsel.class, id);
        assertThat(lagret.getRedusertUtbetalingÅrsaker()).isEqualTo(vedtakVarsel.getRedusertUtbetalingÅrsaker());
    }

    private VedtakVarsel lagVedtakVarsel() {
        VedtakVarsel vedtakVarsel = new VedtakVarsel();
        vedtakVarsel.setAvslagarsakFritekst("avslagsårsakFritekst");
        vedtakVarsel.setBehandlingId(behandling.getId());
        vedtakVarsel.setFritekstbrev("fritekst brev");
        vedtakVarsel.setHarSendtVarselOmRevurdering(true);
        vedtakVarsel.setOverskrift("en overskrift");
        vedtakVarsel.setVedtaksbrev(AUTOMATISK);

        return vedtakVarsel;
    }
}
