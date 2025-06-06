package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

import java.sql.Date;
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

        String sistOpprettet = "(SELECT max(b2.opprettet_tid) FROM Behandling b2 WHERE b2.fagsak_id = f.id)";

        String periodeMedHøySats = "(SELECT 1 FROM UNG_GR ungdomsgrunnlag " +
            "       INNER JOIN UNG_SATS_PERIODE satsperiode ON ungdomsgrunnlag.ung_sats_perioder_id = satsperiode.ung_sats_perioder_id " +
            "       WHERE sats_type = '"+ UngdomsytelseSatsType.HØY.getKode() +"' AND ungdomsgrunnlag.behandling_id = b.id)";

        String reTriggerBeregningHøySats = "(SELECT 1 FROM BEHANDLING_ARSAK behandling_årsak WHERE behandling_årsak.behandling_id = b.id AND behandling_årsak.behandling_arsak_type = 'RE_TRIGGER_BEREGNING_HØY_SATS')";

        Query query = entityManager
            .createNativeQuery(
                "SELECT DISTINCT f.id, foedselsdato FROM GR_PERSONOPPLYSNING gr " +
                    "INNER JOIN PO_INFORMASJON informasjon ON informasjon.id = gr.registrert_informasjon_id " +
                    "INNER JOIN PO_PERSONOPPLYSNING personopplysning ON personopplysning.po_informasjon_id = informasjon.id " +
                    "INNER JOIN BEHANDLING b ON gr.behandling_id = b.id " +
                    "INNER JOIN FAGSAK f ON b.fagsak_id = f.id AND f.bruker_aktoer_id = personopplysning.aktoer_id " +
                    "INNER JOIN UNG_GR_UNGDOMSPROGRAMPERIODE programperiode_gr ON b.id = programperiode_gr.behandling_id " +
                    "INNER JOIN UNG_UNGDOMSPROGRAMPERIODE programperiode ON programperiode_gr.ung_ungdomsprogramperioder_id = programperiode.ung_ungdomsprogramperioder_id " +
                    "WHERE b.opprettet_tid = " + sistOpprettet + // Henter siste behandling
                    "   AND programperiode_gr.aktiv = true" +
                    "   AND gr.aktiv = true" +
                    "   AND f.ytelse_type != 'OBSOLETE'" +
                    "   AND personopplysning.foedselsdato <= :tjuefem_aar_foer_dato " +
                    "   AND programperiode.tom >= date_trunc('month', foedselsdato + interval '301 months') " + // Første dagen i måneden etter 25 års dagen.
                    "   AND f.periode @> date_trunc('month', foedselsdato + interval '301 months')::date" + // Fagsakperioden inneholder endringsdatoen
                    "   AND NOT exists " + periodeMedHøySats + // Idempotens sjekk at vi ikke allerede har beregnet høy sats.
                    "   AND NOT exists " + reTriggerBeregningHøySats, // Idempotens sjekk at vi ikke allerede har trigget beregning av høy sats.
                Tuple.class)
            .setParameter("tjuefem_aar_foer_dato", tjuefemÅrFørDato); // NOSONAR //$NON-NLS-1$
        List<Tuple> resultList = query.getResultList();

        return resultList.stream().collect(Collectors.toMap(this::mapTilFagsak, this::mapTilEndringsdato));
    }

    private Fagsak mapTilFagsak(Tuple tuple) {
        Long fagsakId = tuple.get(0, Long.class);
        return entityManager.find(Fagsak.class, fagsakId);
    }

    private LocalDate mapTilEndringsdato(Tuple tuple) {
        Date fødselsdatoString = tuple.get(1, Date.class);
        return LocalDate.parse(fødselsdatoString.toString()).plusYears(25);
    }
}
