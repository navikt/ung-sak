package no.nav.k9.sak.web.server;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import no.nav.foreldrepenger.dbstoette.DatasourceConfiguration;
import no.nav.vedtak.felles.lokal.dbstoette.ConnectionHandler;
import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;

/**
 * Hacky hjelpeklasse for enkel konvertering av Oracle til Postgres.
 * Tar kun tabeller med indekser/PK/FK/constraints.
 * 
 * Endrer data typer ihht. til planlagt
 * (J/N kolonner til boolean, number(19,0) til BIGINT etc.
 * 
 * stripper sequences som ikke eri bruk
 */
public class EksporterPostgresSqlScript {

    private StringBuffer buf = new StringBuffer(10 * 100 * 1000);

    private DataSource ds;

    List<String> sequences = new ArrayList<String>();

    public EksporterPostgresSqlScript() throws FileNotFoundException {
        var connectionProperties = DatasourceConfiguration.UNIT_TEST.get();
        var dbconp = DBConnectionProperties.finnDefault(connectionProperties).get();
        DatabaseStøtte.settOppJndiForDefaultDataSource(List.of(dbconp));
        var emf = Persistence.createEntityManagerFactory("pu-default");
        var em = emf.createEntityManager();

        for (var e : em.getMetamodel().getEntities()) {
            for (var f : Arrays.asList(e.getJavaType().getDeclaredFields())) {
                if (f.isAnnotationPresent(GeneratedValue.class)) {
                    var ann = f.getAnnotation(GeneratedValue.class);
                    if (Objects.equals(ann.strategy(), GenerationType.SEQUENCE)) {
                        sequences.add(ann.generator().toUpperCase());
                    }
                }
            }
        }

        ds = ConnectionHandler.opprettFra(dbconp);
    }

    public static void main(String[] args) throws Exception {
        var eksport = new EksporterPostgresSqlScript();
        eksport.konverterDbTilPostgresSql();
    }

    /** Konvererer til postgressql DDL */
    public void konverterDbTilPostgresSql() throws Exception {

        List<String> sqls = List.of(
            "ALTER SESSION SET NLS_COMP=LINGUISTIC", // case insensitiv LIKE search i sql
            "ALTER SESSION SET NLS_SORT=BINARY_CI",
            eksporterTabellStruktur(),
            eksporterIndekserOgPrimaryKeys(),
            eksporterForeignKeyDefinitions(),
            eksporterComments());

        try (var conn = ds.getConnection();) {
            for (String sql : sqls) {
                try (var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery();) {
                    while (rs.next()) {
                        buf.append(rs.getString(1)).append("\n");
                    }
                }
            }
        }

        List<String> seqsSql = List.of(
            "ALTER SESSION SET NLS_COMP=LINGUISTIC", // case insensitiv LIKE search i sql
            "ALTER SESSION SET NLS_SORT=BINARY_CI",
            eksporterSequenceDefinitions());

        try (var conn = ds.getConnection();) {
            for (String sql : seqsSql) {
                try (var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery();) {
                    while (rs.next()) {
                        String seqStmt = rs.getString(1);

                        for (var s : List.copyOf(sequences)) {
                            // lukk bort sequences som ikke er i bruk
                            if (seqStmt.matches(".*\\b" + s + "\\b.*")) {
                                buf.append(seqStmt).append("\n");
                                sequences.remove(s); // drop
                                break;
                            }
                        }
                    }
                }
            }
        }

        System.out.println(buf.toString());

        if (!sequences.isEmpty()) {
            System.out.println("\n\n");
            System.out.println("Sequences not found: " + sequences);
        }
    }

    private String eksporterComments() {
        return "select 'comment on table ' || table_name || ' is ''' || comments || ''';' from all_tab_comments\n" +
            "where comments is not null\n" +
            "and owner = sys_context('USERENV', 'CURRENT_SCHEMA')\n" +
            "and table_type='TABLE'\n" +
            "union \n" +
            "select 'comment on column ' || table_name || '.' || column_name || ' is ''' || comments || ''';' from all_col_comments\n" +
            "where comments is not null\n" +
            "and owner = sys_context('USERENV', 'CURRENT_SCHEMA')\n" +
            "order by 1 desc";
    }

    private String eksporterTabellStruktur() {
        return "with cols as (\n" +
            "    select table_name, column_name, data_type, data_precision, data_scale,nullable, column_id, char_length, char_used\n" +
            "    , case -- shitty business of oracle LONG datatypes\n" +
            "       when default_length is null then null\n" +
            "       else\n" +
            "           trim(extractvalue\n" +
            "           ( dbms_xmlgen.getxmltype\n" +
            "             ( 'select data_default from all_tab_cols where table_name = ''' || c.table_name || ''' and column_name = ''' || c.column_name || ''' and owner = ''' || c.owner || '''' )\n"
            +
            "           , '//text()' ))\n" +
            "       end as data_default\n" +
            "    from all_tab_cols c\n" +
            "    where hidden_column = 'NO' and virtual_column='NO' and owner=sys_context('USERENV', 'CURRENT_SCHEMA')\n" +
            "),\n" +
            "cols_pgres as (\n" +
            "    select c.*\n" +
            "        , (case \n" +
            "            when data_type= 'NUMBER' then \n" +
            "                (case \n" +
            "                 when data_precision is null and data_scale is null then 'NUMERIC'\n" +
            "                 when data_precision=19 and data_scale = 0 then 'BIGINT'\n" +
            "                 when data_precision<=9 and data_precision >4 and data_scale=0 then 'INT'\n" +
            "                 when data_precision <4 and data_scale = 0 then 'SMALLINT'\n" +
            "                 else 'NUMERIC(' || coalesce(data_precision, 38)  || ',' || coalesce(data_scale, 0) || ')' end)\n" +
            "            when char_length=1  and data_type IN ('CHAR', 'VARCHAR2') and data_default IN ('''N''', '''J''') then 'BOOLEAN'\n" +
            "            when data_type='VARCHAR2' then 'VARCHAR(' || char_length || ')'\n" +
            "            when data_type = 'CHAR' then 'VARCHAR(' || char_length || ')'\n" +
            "            when data_type='CLOB' then 'TEXT'\n" +
            "            when data_type='RAW' then 'UUID' -- alltid riktig her , bruker ikke RAW til annet\n" +
            "            when data_type like 'TIMESTAMP%' then data_type\n" +
            "            when data_type ='DATE' then 'TIMESTAMP(0)'\n" +
            "        end)  pgres_col\n" +
            "        , (case \n" +
            "            when data_default= 'systimestamp' or data_default='SYSTIMESTAMP' then 'CURRENT_TIMESTAMP'\n" +
            "            when data_default = 'sysdate' or data_default='SYSDATE' then 'CURRENT_TIMESTAMP'\n" +
            "            when data_default like 'to_date%' or data_default like 'TO_DATE%' then upper(data_default)\n" +
            "            when data_default like '%fpsak%' or data_default like 'FPSAK%' then null -- fjern magisk verdi\n" +
            "            when char_length=1  and data_type IN ('CHAR', 'VARCHAR2') and data_default IN ('''N''', '''J''') then (case data_default when 'J' then 'true' else 'false' end) -- oracle J/N char to boolean\n"
            +
            "            else data_default\n" +
            "        end)  pgres_def\n" +
            "        , (case nullable when 'N' then 'NOT NULL' end) as pgres_null\n" +
            "    from cols c order by c.table_name, c.column_id\n" +
            ")\n" +
            ", table_defs as (\n" +
            "  select 'create table if not exists  '|| table_name || '(' ||\n" +
            "            listagg(column_name || ' ' || pgres_col || (case when pgres_def is not null then ' DEFAULT ' || pgres_def end) || (case when pgres_null is not null then ' ' || pgres_null end) , ', ' || chr(10))\n"
            +
            "            within group (order by column_id)\n" +
            "            over (partition by table_name)\n" +
            "        ||');' || chr(10) as sql\n" +
            "  from cols_pgres\n" +
            "  )\n" +
            "select distinct sql from table_defs order by 1";
    }

    private String eksporterIndekserOgPrimaryKeys() {
        return "--- INDEXES and PRIMARY KEYS\n" +
            "with idx as (SELECT i.index_name, i.table_name, i.uniqueness, ic.column_name, ic.column_position\n" +
            "from user_indexes i \n" +
            "inner join user_ind_columns ic on ic.index_name=i.index_name and ic.table_name=i.table_name\n" +
            "where index_type='NORMAL' and i.table_type='TABLE'\n" +
            ")\n" +
            ", idx_cols as (select idx.*, listagg(column_name, ', ') within group (order by column_position) over(partition by table_name, index_name) as col_list from idx\n"
            +
            ")\n" +
            "select distinct 'create '|| (case uniqueness when 'UNIQUE' then 'UNIQUE' end ) || ' index '  || index_name || ' on ' || table_name || ' (' || col_list  || ');'\n"
            +
            " as sql from idx_cols where index_name not like 'PK_%'\n" +
            "union  -- primary keys\n" +
            "select distinct 'create UNIQUE index '  || index_name || ' on ' || table_name || ' (' || col_list  || ');' \n" +
            "  || 'alter table ' || table_name || ' add constraint PK_' || table_name || ' primary key using index ' || index_name || ';' || chr(10)\n" +
            " from idx_cols where index_name like 'PK_%' and uniqueness='UNIQUE'\n" +
            "";
    }

    private String eksporterForeignKeyDefinitions() {
        return "-- FOREIGN KEYS\n" +
            "with fks as (\n" +
            "SELECT a.table_name, a.column_name, a.position, a.constraint_name, c.owner, \n" +
            "       -- referenced pk\n" +
            "       c.r_owner, c_pk.table_name r_table_name, c_pk.constraint_name r_pk,\n" +
            "       -- refrenced cols\n" +
            "       b.column_name r_column_name\n" +
            "  FROM all_cons_columns a\n" +
            "  JOIN all_constraints c ON a.owner = c.owner AND a.constraint_name = c.constraint_name\n" +
            "  JOIN all_constraints c_pk ON c.r_owner = c_pk.owner AND c.r_constraint_name = c_pk.constraint_name\n" +
            "  JOIN all_cons_columns b ON b.owner = c_pk.owner and b.constraint_name = c_pk.constraint_name and b.position = a.position\n" +
            " WHERE c.constraint_type = 'R' and c.owner=sys_context('USERENV', 'CURRENT_SCHEMA')\n" +
            " ORDER BY a.table_name, a.constraint_name, a.position)\n" +
            " select distinct 'alter table ' || table_name || ' add constraint '|| constraint_name || ' foreign key (' ||\n" +
            "listagg(column_name, ', ') within group (order by position) over (partition by table_name, constraint_name) || \n" +
            "') references ' || r_table_name || '(' ||\n" +
            "listagg(r_column_name, ', ') within group (order by position) over (partition by table_name, constraint_name) || ');'\n" +
            " as sql from fks";
    }

    private String eksporterSequenceDefinitions() {
        return "with seqs as (\n" +
            "select * from all_sequences where all_sequences.sequence_owner=sys_context('USERENV', 'CURRENT_SCHEMA'))\n" +
            "select 'create sequence if not exists ' || upper(sequence_name) || ' increment by ' || increment_by || ' minvalue 1000000; ' as sql from seqs";
    }
}
