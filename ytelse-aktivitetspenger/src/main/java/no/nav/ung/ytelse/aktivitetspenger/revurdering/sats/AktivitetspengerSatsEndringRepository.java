package no.nav.ung.ytelse.aktivitetspenger.revurdering.sats;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Dependent
public class AktivitetspengerSatsEndringRepository {

    private EntityManager entityManager;

    AktivitetspengerSatsEndringRepository() {
    }

    @Inject
    public AktivitetspengerSatsEndringRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Map<Fagsak, LocalDate> hentFagsakerMedBrukereSomFyller25ÅrFraDato(LocalDate dato) {
        LocalDate tjuefemÅrFørDato = dato.minusYears(25);

        String sistOpprettet = "(SELECT max(b2.opprettet_tid) FROM Behandling b2 WHERE b2.fagsak_id = f.id)";

        String periodeMedHøySats = "(SELECT 1 FROM GR_AVP avp_gr " +
            "       INNER JOIN AVP_SATS_PERIODE satsperiode ON avp_gr.avp_sats_perioder_id = satsperiode.avp_sats_perioder_id " +
            "       WHERE satsperiode.sats_type = '" + UngdomsytelseSatsType.HØY.getKode() + "' AND avp_gr.behandling_id = b.id)";

        String reTriggerBeregningHøySats = "(SELECT 1 FROM BEHANDLING_ARSAK behandling_årsak WHERE behandling_årsak.behandling_id = b.id AND behandling_årsak.behandling_arsak_type = 'RE_TRIGGER_BEREGNING_HØY_SATS')";

        Query query = entityManager
            .createNativeQuery(
                "SELECT DISTINCT f.id, foedselsdato FROM GR_PERSONOPPLYSNING gr " +
                    "INNER JOIN PO_INFORMASJON informasjon ON informasjon.id = gr.registrert_informasjon_id " +
                    "INNER JOIN PO_PERSONOPPLYSNING personopplysning ON personopplysning.po_informasjon_id = informasjon.id " +
                    "INNER JOIN BEHANDLING b ON gr.behandling_id = b.id " +
                    "INNER JOIN FAGSAK f ON b.fagsak_id = f.id AND f.bruker_aktoer_id = personopplysning.aktoer_id " +
                    "INNER JOIN AKT_SOEKT_PERIODE søkt_periode ON b.id = søkt_periode.behandling_id " +
                    "WHERE b.opprettet_tid = " + sistOpprettet +
                    "   AND gr.aktiv = true" +
                    "   AND f.ytelse_type = '" + FagsakYtelseType.AKTIVITETSPENGER.getKode() + "'" +
                    "   AND personopplysning.foedselsdato <= :tjuefem_aar_foer_dato " +
                    "   AND daterange(søkt_periode.fom, søkt_periode.tom, '[]') @> date_trunc('month', foedselsdato + interval '301 months')::date" +
                    "   AND f.periode @> date_trunc('month', foedselsdato + interval '301 months')::date" +
                    "   AND NOT exists " + periodeMedHøySats +
                    "   AND NOT exists " + reTriggerBeregningHøySats,
                Tuple.class)
            .setParameter("tjuefem_aar_foer_dato", tjuefemÅrFørDato);
        List<Tuple> resultList = query.getResultList();

        return resultList.stream().collect(Collectors.toMap(this::mapTilFagsak, this::mapTilEndringsdato));
    }

    private Fagsak mapTilFagsak(Tuple tuple) {
        Long fagsakId = tuple.get(0, Long.class);
        return entityManager.find(Fagsak.class, fagsakId);
    }

    private LocalDate mapTilEndringsdato(Tuple tuple) {
        LocalDate fødselsdato = tuple.get(1, LocalDate.class);
        return fødselsdato.plusYears(25);
    }
}
