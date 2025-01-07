package no.nav.ung.sak.hendelsemottak.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.typer.AktørId;

@Dependent
public class FinnFagsakerForAktørTjeneste {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;

    @Inject
    public FinnFagsakerForAktørTjeneste(EntityManager entityManager, FagsakRepository fagsakRepository) {
        this.entityManager = entityManager;
        this.fagsakRepository = fagsakRepository;
    }

    public List<Fagsak> hentRelevantFagsakForAktørSomBarnAvSøker(AktørId aktørId, LocalDate relevantDato) {
        Query query = entityManager.createNativeQuery(
                "SELECT f.* FROM GR_PERSONOPPLYSNING gr " +
                    "inner join PO_INFORMASJON informasjon on informasjon.id = gr.registrert_informasjon_id " +
                    "inner join PO_RELASJON relasjon on relasjon.po_informasjon_id = informasjon.id " +
                    "inner join BEHANDLING b on gr.behandling_id = b.id " +
                    "inner join FAGSAK f on b.fagsak_id = f.id " +
                    "WHERE f.ytelse_type != 'OBSOLETE'" +
                    "and relasjon.relasjonsrolle = :relasjonsrolle " +
                    "and relasjon.til_aktoer_id = :aktoer_id " +
                    "and f.periode && daterange(cast(:dato as date), cast(:dato as date), '[]') = true", //$NON-NLS-1$
                Fagsak.class)
            .setParameter("relasjonsrolle", RelasjonsRolleType.BARN.getKode())
            .setParameter("aktoer_id", aktørId.getId())
            .setParameter("dato", relevantDato); // NOSONAR //$NON-NLS-1$
        return query.getResultList();
    }

    public Optional<Fagsak> hentRelevantFagsakForAktørSomSøker(AktørId aktør, LocalDate relevantDato) {
        return fagsakRepository.hentForBruker(aktør).stream().filter(f -> f.getPeriode().overlapper(relevantDato, AbstractLocalDateInterval.TIDENES_ENDE)).findFirst();
    }


}
