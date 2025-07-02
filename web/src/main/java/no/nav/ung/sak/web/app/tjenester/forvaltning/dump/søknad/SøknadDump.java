package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.søknad;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

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
    public void dump(DumpMottaker dumpMottaker) {
        String sql = ""
            + "select f.saksnummer"
            + ", f.id as fagsak_id"
            + ", gr.behandling_id"
            + ", b.behandling_status"
            + ", b.behandling_resultat_type"
            + ", so.id as so_soeknad_id"
            + ", so.startdato"
            + ", so.mottatt_dato"
            + ", so.elektronisk_registrert"
            + ", so.begrunnelse_for_sen_innsending"
            + ", so.sprak_kode"
            + ", so.journalpost_id"
            + ", so.soeknad_id as mottatt_soeknad_id"
            + ", replace(cast(so.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + " from "
            + " gr_soeknad gr"
            + " inner join behandling b on b.id=gr.behandling_id and gr.aktiv=true"
            + " inner join fagsak f on f.id=b.fagsak_id"
            + " inner join so_soeknad so on gr.soeknad_id=so.id"
            + " where  f.saksnummer=:saksnummer"
            + " order by 1, 2, 3, so.opprettet_tid";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("søknad.csv");
            dumpMottaker.write(output.get());
        }
    }
}
