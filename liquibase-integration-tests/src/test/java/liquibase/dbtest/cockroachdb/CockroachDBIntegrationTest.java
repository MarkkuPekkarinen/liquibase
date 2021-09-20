package liquibase.dbtest.cockroachdb;

import liquibase.Scope;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.dbtest.AbstractIntegrationTest;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Column;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CockroachDBIntegrationTest extends AbstractIntegrationTest {

    public CockroachDBIntegrationTest() throws Exception {
        super("cockroachdb", DatabaseFactory.getInstance().getDatabase("cockroachdb"));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "CREATE USER IF NOT EXISTS lbuser"
        );
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "CREATE DATABASE IF NOT EXISTS lbcat"
        );
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "CREATE DATABASE IF NOT EXISTS lbcat2"
        );
        // Create schemas for tests testRerunDiffChangeLogAltSchema
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "CREATE SCHEMA IF NOT EXISTS lbcat2"
        );
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "GRANT ALL ON DATABASE lbcat TO lbuser"
        );//
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "GRANT ALL ON DATABASE lbcat2 TO lbuser"
        );
        // Create schemas for tests testRerunDiffChangeLogAltSchema
        ((JdbcConnection) getDatabase().getConnection()).getUnderlyingConnection().createStatement().executeUpdate(
                "GRANT ALL ON SCHEMA lbcat2 TO lbuser"
        );

        getDatabase().commit();

    }

    @Override
    protected boolean isDatabaseProvidedByTravisCI() {
        return true;
    }

    @Test
    public void snapshot() throws Exception {
        if (getDatabase() == null) {
            return;
        }

        runCompleteChangeLog();
        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(getDatabase().getDefaultSchema(), getDatabase(), new SnapshotControl(getDatabase()));
        System.out.println(snapshot);
    }

    @Test
    public void descPrimaryKey() throws Exception {
        if (getDatabase() == null) {
            return;
        }
        final Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", getDatabase());

        executor.execute(new RawSqlStatement("DROP TABLE IF EXISTS pk"));

        executor.execute(new RawSqlStatement("CREATE TABLE pk (\n" +
                "a INT8 NOT NULL,\n" +
                "b INT8 NOT NULL,\n" +
                "c INT8 NOT NULL,\n" +
                "d INT8 NOT NULL,\n" +
                "CONSTRAINT \"primary\" PRIMARY KEY (a ASC, b ASC, c DESC)\n" +
                ")"));

        DatabaseSnapshot snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(getDatabase().getDefaultSchema(), getDatabase(), new SnapshotControl(getDatabase()));
        PrimaryKey pk = snapshot.get(new PrimaryKey().setTable(new Table().setName("pk")).setName("primary"));

        List<Column> columns = pk.getColumns();
        assertEquals("a", columns.get(0).getName());
        assertNull(columns.get(0).getDescending());
        assertEquals("b", columns.get(1).getName());
        assertNull(columns.get(1).getDescending());
        assertEquals("c", columns.get(2).getName());
        assertTrue(columns.get(2).getDescending());
    }

    @Test
    @Override
    public void testRunUpdateOnOldChangelogTableFormat() throws Exception {
        // This test is skipped because CockroachDB doesn't allow the columns of the same table to be altered
        // concurrently.
        // See https://github.com/cockroachdb/cockroach/issues/47137
    }
}
