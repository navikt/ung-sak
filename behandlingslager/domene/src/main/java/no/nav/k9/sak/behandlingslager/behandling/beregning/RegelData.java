package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.PersistenceException;

import org.hibernate.engine.jdbc.ClobProxy;

/** Wrapper regel input/sporing data. */
public class RegelData {
    final Clob clob;

    private final AtomicReference<String> ref = new AtomicReference<>();

    public RegelData(Clob clob) {
        this.clob = Objects.requireNonNull(clob, "mangler clob");
    }

    public RegelData(String str) {
        this.clob = createProxy(str);
        this.ref.set(str);
    }

    public String asString() {
        var str = ref.get();
        if (str != null && !str.isBlank()) {
            return str; // quick return, deserialisert tidligere
        }

        if (clob == null || (str != null && str.isEmpty())) {
            return null; // quick return, har ikke eller er tom
        }

        str = ""; // dummy value for å signalisere at er allerede deserialisert
        try {
            BufferedReader in = new BufferedReader(clob.getCharacterStream());
            String line;
            StringBuilder sb = new StringBuilder(2048);
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            str = sb.toString();
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Kunne ikke lese payload: ", e);
        }
        ref.set(str);
        return str;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !obj.getClass().equals(this.getClass()))
            return false;
        var other = (RegelData) obj;
        return Objects.equals(asString(), other.asString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(asString());
    }

    static Clob createProxy(String input) {
        return input.isEmpty() ? null : ClobProxy.generateProxy(input);
    }
}