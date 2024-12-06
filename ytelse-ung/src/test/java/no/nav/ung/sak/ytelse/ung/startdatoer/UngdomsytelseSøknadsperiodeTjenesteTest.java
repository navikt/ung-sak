package no.nav.ung.sak.ytelse.ung.startdatoer;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriode;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseSøknadsperiodeTjenesteTest {

    private UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;

    @Inject
    private EntityManager em;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(em);
        søknadsperiodeRepository = new UngdomsytelseSøknadsperiodeRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        ungdomsytelseSøknadsperiodeTjeneste = new UngdomsytelseSøknadsperiodeTjeneste(søknadsperiodeRepository,
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            behandlingRepository
            );

        var fom = LocalDate.now();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(64));
        em.persist(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_begrense_ungdomsprogramperiode_uten_opphør_til_fagsakpeiode_tom() {
        var fom = LocalDate.now();
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, TIDENES_ENDE)));
        var søknadsperioder = List.of(new UngdomsytelseSøktStartdato(fom, new JournalpostId(12455L)));
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknader(behandling.getId(), new UngdomsytelseSøknader(søknadsperioder));

        var periode = ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandling.getId());

        assertThat(periode.size()).isEqualTo(1);
        var enestePeriode = periode.getFirst();
        assertThat(enestePeriode.getTomDato()).isEqualTo(fom.plusWeeks(64));
    }

    @Test
    void kun_returnere_sammenhengende_perioder_som_er_søkt_om() {
        var fom = LocalDate.now();
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
            new UngdomsprogramPeriode(fom, fom.plusWeeks(2)),
            new UngdomsprogramPeriode(fom.plusWeeks(2).plusDays(1), fom.plusWeeks(3)),
            // Hull i perioder
            new UngdomsprogramPeriode(fom.plusWeeks(4), fom.plusWeeks(5))));
        var søknadsperioder = List.of(new UngdomsytelseSøktStartdato(fom, new JournalpostId(12455L)));
        søknadsperiodeRepository.lagre(behandling.getId(), søknadsperioder);
        søknadsperiodeRepository.lagreRelevanteSøknader(behandling.getId(), new UngdomsytelseSøknader(søknadsperioder));

        var periode = ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandling.getId());

        assertThat(periode.size()).isEqualTo(1);
        var enestePeriode = periode.getFirst();
        assertThat(enestePeriode.getTomDato()).isEqualTo(fom.plusWeeks(3));
    }
}
