package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.søknad;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef
public class SøknadDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    SøknadDump() {
        // for proxys
    }

    @Inject
    SøknadDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = ""
            + "select f.saksnummer"
            + ", f.id as fagsak_id"
            + ", gr.behandling_id"
            + ", b.behandling_status"
            + ", b.behandling_resultat_type"
            + ", so.id as so_soeknad_id"
            + ", so.soeknadsdato"
            + ", so.fom"
            + ", so.tom"
            + ", so.mottatt_dato"
            + ", so.tilleggsopplysninger"
            + ", so.elektronisk_registrert"
            + ", so.begrunnelse_for_sen_innsending"
            + ", so.er_endringssoeknad"
            + ", so.bruker_rolle"
            + ", so.sprak_kode"
            + ", so.journalpost_id"
            + ", so.soeknad_id as mottatt_soeknad_id"
            + ", replace(cast(so.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + ", soan.rolle as annen_part_rolle"
            + ", soan.aktoer_id as annen_part_aktoer_id"
            + ", soan.situasjon_kode as annen_part_situasjon_kode"
            + ", soan.tilleggsopplysninger as annen_part_tilleggsopplysninger"
            + " from "
            + " gr_soeknad gr"
            + " inner join behandling b on b.id=gr.behandling_id and gr.aktiv=true"
            + " inner join fagsak f on f.id=b.fagsak_id"
            + " inner join so_soeknad so on gr.soeknad_id=so.id"
            + " left outer join SO_SOEKNAD_ANGITT_PERSON soan on soan.soeknad_id=so.id"
            + " where  f.saksnummer=:saksnummer"
            + " order by 1, 2, 3, so.opprettet_tid";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "søknad.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
