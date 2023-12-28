package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class OpptjeningsresultatDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    OpptjeningsresultatDump() {
        // for proxys
    }

    @Inject
    OpptjeningsresultatDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select "
            + "  f.saksnummer "
            + ", rs.behandling_id "
            + " , replace(cast(rs.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + ", opp.opptjent_periode "
            + ", opp.fom as opp_fom "
            + ", opp.tom as opp_tom "
            + ", oa.opptjeningsperiode_id "
            + ", oa.fom as opp_akt_fom "
            + ", oa.tom as opp_akt_tom "
            + ", oa.aktivitet_type "
            + ", cast(oa.aktivitet_referanse as varchar) aktivitet_referanse"
            + ", oa.klassifisering "
            + ", oa.referanse_type "
            + ", replace(cast(oa.opprettet_tid as varchar), ' ', 'T') oa_opprettet_tid"
            + " from rs_opptjening rs "
            + "  inner join behandling b on b.id=rs.behandling_id "
            + "  inner join fagsak f on f.id=b.fagsak_id "
            + "  inner join opptjening opp on opp.opptjening_resultat_id=rs.id "
            + "  inner join OPPTJENING_AKTIVITET oa on oa.opptjeningsperiode_id=opp.id "
            + " where rs.aktiv=true and f.saksnummer=:saksnummer"
            + " order by f.saksnummer, rs.behandling_id, oa.opprettet_tid";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("opptjeningsresultat.csv");
            dumpMottaker.write(output.get());
        }
    }
}
