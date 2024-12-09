package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Dependent
public class SatsEndringRepository {

    private EntityManager entityManager;

    SatsEndringRepository() {
        // for CDI proxy
    }

    @Inject
    public SatsEndringRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Map<Fagsak, LocalDate> hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate dato) {
        LocalDate tjuefemÅrFørDato = dato.minusYears(25);

        //language=PostgreSQL
        String sistOpprettet = "(SELECT max(b2.opprettet_tid) FROM Behandling b2 WHERE b2.fagsak_id=f.id ORDER BY b2.opprettet_tid DESC)";

        Query query = entityManager
            .createNativeQuery(
                "SELECT DISTINCT f fagsakTilVurdering, foedselsdato FROM GR_PERSONOPPLYSNING gr " +
                    "INNER JOIN PO_INFORMASJON informasjon ON informasjon.id = gr.registrert_informasjon_id " +
                    "INNER JOIN PO_PERSONOPPLYSNING personopplysning ON personopplysning.po_informasjon_id = informasjon.id " +
                    "INNER JOIN BEHANDLING b ON gr.behandling_id = b.id " +
                    "INNER JOIN FAGSAK f ON b.fagsak_id = f.id AND f.bruker_aktoer_id = personopplysning.aktoer_id " +
                    "INNER JOIN UNG_GR_UNGDOMSPROGRAMPERIODE programperiode_gr ON b.id = programperiode_gr.behandling_id " +
                    "INNER JOIN UNG_UNGDOMSPROGRAMPERIODE programperiode ON programperiode_gr.ung_ungdomsprogramperioder_id = programperiode.ung_ungdomsprogramperioder_id " +
                    "WHERE b.opprettet_tid = " + sistOpprettet + // Henter siste behandling
                    "   AND personopplysning.foedselsdato <= :tjuefem_aar_foer_dato " +
                    "   AND programperiode.tom >= date_trunc('month', foedselsdato + interval '301 months') " + // Første dagen i måneden etter 25 års dagen.
                    "   AND programperiode_gr.aktiv = true" +
                    "   AND f.periode @> date_trunc('month', foedselsdato + interval '301 months')" + // Fagsakperioden inneholder endringsdatoen
                    // Idempotens sjekk at vi ikke allerede har beregnet høy sats.
                    "   AND NOT exists (SELECT 1 FROM UNG_GR ungdomsgrunnlag " +
                    "       INNER JOIN UNG_SATS_PERIODE satsperiode ON ungdomsgrunnlag.ung_sats_perioder_id = satsperiode.id " +
                    "       WHERE sats_type = 'HOY' AND ungdomsgrunnlag.behandling_id = b.id)", //$NON-NLS-1$
                Tuple.class)
            .setParameter("tjuefem_aar_foer_dato", tjuefemÅrFørDato); // NOSONAR //$NON-NLS-1$
        List<Tuple> resultList = query.getResultList();
        return resultList.stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get("fagsakTilVurdering", Fagsak.class),
                tuple -> tuple.get("foedselsdato", LocalDate.class).plusMonths(1).withDayOfMonth(1)) // Endringsdato
            );
    }
}
